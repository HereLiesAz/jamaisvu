const ACCESS_TTL_SECONDS = 6 * 60 * 60;
const REFRESH_TTL_SECONDS = 30 * 24 * 60 * 60;
const MAX_IMAGE_BYTES = 750_000;
const MAX_MEDIA_STORAGE_BYTES = 400_000_000;

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") return cors(new Response(null, { status: 204 }));

    try {
      const url = new URL(request.url);
      const path = url.pathname.replace(/\/+$/, "") || "/";

      if (request.method === "GET" && path === "/health") {
        return json({ ok: true, service: "jamaisvu-api" });
      }
      if (request.method === "POST" && path === "/v1/auth/signup") return signUp(request, env);
      if (request.method === "POST" && path === "/v1/auth/signin") return signIn(request, env);
      if (request.method === "POST" && path === "/v1/auth/refresh") return refreshSession(request, env);
      if (request.method === "GET" && path === "/v1/snapshot") return snapshot(request, env);
      if (request.method === "POST" && path === "/v1/gems") return createGem(request, env);
      if (request.method === "POST" && path === "/v1/images") return uploadImage(request, env);
      if (request.method === "GET" && path.startsWith("/v1/images/")) return getImage(path, env);

      const relation = path.match(/^\/v1\/(saved|visited|follows)\/([^/]+)$/);
      if (relation && (request.method === "PUT" || request.method === "DELETE")) {
        return setRelation(request, env, relation[1], decodeURIComponent(relation[2]));
      }

      throw new HttpError(404, "Not found");
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const message = status === 500 ? "Backend request failed" : error.message;
      if (status === 500) console.error(error);
      return json({ message }, status);
    }
  },
};

async function signUp(request, env) {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const passwordProof = String(body.passwordProof || "");
  const handle = normalizeHandle(body.handle);
  const city = String(body.city || "").trim();

  if (!email || !email.includes("@")) throw new HttpError(400, "Enter a valid email address");
  if (!/^[0-9a-f]{64}$/i.test(passwordProof)) throw new HttpError(400, "Invalid password proof");
  if (!/^[A-Za-z0-9_.-]{2,30}$/.test(handle)) {
    throw new HttpError(400, "Handle must be 2–30 letters, numbers, dots, dashes, or underscores");
  }

  const existing = await env.DB.prepare(
    "SELECT email, handle FROM users WHERE lower(email) = ? OR lower(handle) = ? LIMIT 1"
  ).bind(email, handle.toLowerCase()).first();
  if (existing?.email?.toLowerCase() === email) throw new HttpError(409, "That email already has an account");
  if (existing) throw new HttpError(409, "That handle is already taken");

  const id = crypto.randomUUID();
  const salt = base64UrlEncode(randomBytes(16));
  const passwordHash = await hashPasswordProof(passwordProof, salt);

  try {
    await env.DB.prepare(
      `INSERT INTO users (id, email, password_hash, password_salt, handle, city, created_at)
       VALUES (?, ?, ?, ?, ?, ?, unixepoch())`
    ).bind(id, email, passwordHash, salt, handle, city).run();
  } catch (error) {
    console.error(error);
    throw new HttpError(409, "That email or handle is already in use");
  }

  return json(await createSession(env, { id, email }));
}

async function signIn(request, env) {
  const body = await readJson(request);
  const email = normalizeEmail(body.email);
  const passwordProof = String(body.passwordProof || "");
  if (!email || !/^[0-9a-f]{64}$/i.test(passwordProof)) throw new HttpError(400, "Email and password proof are required");

  const user = await env.DB.prepare(
    "SELECT id, email, password_hash, password_salt FROM users WHERE lower(email) = ? LIMIT 1"
  ).bind(email).first();
  if (!user?.password_hash || !user?.password_salt) throw new HttpError(401, "Incorrect email or password");

  const actual = await hashPasswordProof(passwordProof, user.password_salt);
  if (!constantTimeTextEqual(actual, user.password_hash)) throw new HttpError(401, "Incorrect email or password");

  await env.DB.prepare("DELETE FROM sessions WHERE refresh_expires_at <= unixepoch()").run();
  return json(await createSession(env, user));
}

async function refreshSession(request, env) {
  const body = await readJson(request);
  const refreshToken = String(body.refresh_token || body.refreshToken || "");
  if (!refreshToken) throw new HttpError(400, "Refresh token is required");

  const refreshHash = await sha256Hex(refreshToken);
  const row = await env.DB.prepare(
    `SELECT s.id AS session_id, u.id, u.email
     FROM sessions s JOIN users u ON u.id = s.user_id
     WHERE s.refresh_hash = ? AND s.refresh_expires_at > unixepoch()
     LIMIT 1`
  ).bind(refreshHash).first();
  if (!row) throw new HttpError(401, "Session expired");

  await env.DB.prepare("DELETE FROM sessions WHERE id = ?").bind(row.session_id).run();
  return json(await createSession(env, row));
}

async function snapshot(request, env) {
  const user = await optionalAuth(request, env);

  const [profilesResult, gemsResult] = await env.DB.batch([
    env.DB.prepare(
      `SELECT u.id, u.handle, u.city, u.bio, u.avatar_url,
        (SELECT COUNT(*) FROM gems g WHERE g.author_id = u.id) AS gem_count,
        (SELECT COUNT(*) FROM follows f WHERE f.following_id = u.id) AS follower_count,
        (SELECT COUNT(*) FROM follows f WHERE f.follower_id = u.id) AS following_count
       FROM users u
       ORDER BY gem_count DESC, u.handle COLLATE NOCASE`
    ),
    env.DB.prepare(
      `SELECT g.id, g.title, g.city, g.neighborhood, g.category, g.tags, g.tip,
              g.image_url, g.latitude, g.longitude, g.author_id, g.created_at,
              COALESCE(u.handle, 'local') AS handle
       FROM gems g LEFT JOIN users u ON u.id = g.author_id
       ORDER BY g.created_at DESC, g.title COLLATE NOCASE
       LIMIT 1000`
    ),
  ]);

  const profiles = (profilesResult.results || []).map((row) => ({
    id: row.id,
    handle: row.handle,
    city: row.city || "",
    bio: row.bio || "",
    avatarUrl: row.avatar_url || null,
    gemCount: Number(row.gem_count || 0),
    followerCount: Number(row.follower_count || 0),
    followingCount: Number(row.following_count || 0),
  }));

  const gems = (gemsResult.results || []).map((row) => ({
    id: row.id,
    title: row.title,
    city: row.city,
    neighborhood: row.neighborhood || "",
    category: row.category,
    tags: splitTags(row.tags),
    tip: row.tip || row.tags || "",
    username: `@${String(row.handle || "local").replace(/^@/, "")}`,
    image: row.image_url || "",
    authorId: row.author_id || null,
    createdAt: row.created_at ? new Date(Number(row.created_at) * 1000).toISOString() : null,
    latitude: row.latitude == null ? null : Number(row.latitude),
    longitude: row.longitude == null ? null : Number(row.longitude),
  }));

  let savedIds = [];
  let visitedIds = [];
  let followingIds = [];
  if (user) {
    const [saved, visited, following] = await env.DB.batch([
      env.DB.prepare("SELECT gem_id FROM saved_gems WHERE user_id = ?").bind(user.id),
      env.DB.prepare("SELECT gem_id FROM visited_gems WHERE user_id = ?").bind(user.id),
      env.DB.prepare("SELECT following_id FROM follows WHERE follower_id = ?").bind(user.id),
    ]);
    savedIds = (saved.results || []).map((row) => row.gem_id);
    visitedIds = (visited.results || []).map((row) => row.gem_id);
    followingIds = (following.results || []).map((row) => row.following_id);
  }

  return json({ gems, profiles, savedIds, visitedIds, followingIds });
}

async function setRelation(request, env, kind, targetId) {
  const user = await requireAuth(request, env);
  if (!targetId) throw new HttpError(400, "Missing target id");

  const config = {
    saved: ["saved_gems", "user_id", "gem_id"],
    visited: ["visited_gems", "user_id", "gem_id"],
    follows: ["follows", "follower_id", "following_id"],
  }[kind];
  if (!config) throw new HttpError(404, "Not found");
  if (kind === "follows" && targetId === user.id) throw new HttpError(400, "You cannot follow yourself");

  const [table, left, right] = config;
  if (request.method === "PUT") {
    await env.DB.prepare(
      `INSERT OR IGNORE INTO ${table} (${left}, ${right}, created_at) VALUES (?, ?, unixepoch())`
    ).bind(user.id, targetId).run();
  } else {
    await env.DB.prepare(`DELETE FROM ${table} WHERE ${left} = ? AND ${right} = ?`)
      .bind(user.id, targetId).run();
  }
  return json({ ok: true });
}

async function createGem(request, env) {
  const user = await requireAuth(request, env);
  const body = await readJson(request);
  const title = String(body.title || "").trim();
  const city = String(body.city || "").trim();
  const neighborhood = String(body.neighborhood || "").trim();
  const category = String(body.category || "").trim();
  const tip = String(body.tip || "").trim();
  const imageUrl = body.imageUrl ? String(body.imageUrl) : null;
  const tags = Array.isArray(body.tags) ? body.tags.map(String).map((s) => s.trim()).filter(Boolean) : [category].filter(Boolean);
  const latitude = finiteNumberOrNull(body.latitude);
  const longitude = finiteNumberOrNull(body.longitude);

  if (!title || !city || !category || !tip) throw new HttpError(400, "Place name, city, category, and tip are required");

  const id = crypto.randomUUID();
  await env.DB.prepare(
    `INSERT INTO gems
      (id, author_id, title, city, neighborhood, category, tags, tip, image_url, latitude, longitude, source, created_at, updated_at)
     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'user', unixepoch(), unixepoch())`
  ).bind(
    id, user.id, title, city, neighborhood, category, tags.join("; "), tip, imageUrl,
    latitude, longitude
  ).run();

  const handle = await env.DB.prepare("SELECT handle FROM users WHERE id = ?").bind(user.id).first("handle");
  return json({
    id, title, city, neighborhood, category, tags, tip,
    username: `@${handle}`,
    image: imageUrl || "",
    authorId: user.id,
    createdAt: new Date().toISOString(),
    latitude,
    longitude,
  }, 201);
}

async function uploadImage(request, env) {
  const user = await requireAuth(request, env);
  const contentType = request.headers.get("content-type") || "application/octet-stream";
  if (!contentType.toLowerCase().startsWith("image/")) throw new HttpError(415, "Only image uploads are accepted");

  const declared = Number(request.headers.get("content-length") || 0);
  if (declared > MAX_IMAGE_BYTES) throw new HttpError(413, "Image is too large after compression");

  const bytes = await request.arrayBuffer();
  if (bytes.byteLength > MAX_IMAGE_BYTES) throw new HttpError(413, "Image is too large after compression");

  const usage = await env.MEDIA.prepare("SELECT COALESCE(SUM(byte_size), 0) AS total FROM images").first();
  const currentBytes = Number(usage?.total || 0);
  if (currentBytes + bytes.byteLength > MAX_MEDIA_STORAGE_BYTES) {
    throw new HttpError(507, "Free photo storage is full");
  }

  const key = crypto.randomUUID();
  await env.MEDIA.prepare(
    "INSERT INTO images (id, owner_id, content_type, body, byte_size, created_at) VALUES (?, ?, ?, ?, ?, unixepoch())"
  ).bind(key, user.id, contentType, bytes, bytes.byteLength).run();
  const url = new URL(request.url);
  return json({ url: `${url.origin}/v1/images/${key}` }, 201);
}

async function getImage(path, env) {
  const key = decodeURIComponent(path.slice("/v1/images/".length));
  if (!key || key.includes("/") || key.includes("..")) throw new HttpError(400, "Invalid image key");
  const row = await env.MEDIA.prepare(
    "SELECT content_type, body FROM images WHERE id = ? LIMIT 1"
  ).bind(key).first();
  if (!row) throw new HttpError(404, "Image not found");

  const headers = new Headers({
    "content-type": row.content_type,
    "cache-control": "public, max-age=31536000, immutable",
  });
  return cors(new Response(new Uint8Array(row.body), { headers }));
}

async function optionalAuth(request, env) {
  const auth = request.headers.get("authorization") || "";
  if (!auth) return null;
  return requireAuth(request, env);
}

async function requireAuth(request, env) {
  const auth = request.headers.get("authorization") || "";
  const match = auth.match(/^Bearer\s+(.+)$/i);
  if (!match) throw new HttpError(401, "Sign in required");

  const accessHash = await sha256Hex(match[1]);
  const user = await env.DB.prepare(
    `SELECT u.id, u.email, u.handle, u.city, u.bio, u.avatar_url
     FROM sessions s JOIN users u ON u.id = s.user_id
     WHERE s.access_hash = ? AND s.access_expires_at > unixepoch()
     LIMIT 1`
  ).bind(accessHash).first();
  if (!user) throw new HttpError(401, "Session expired");
  return user;
}

async function createSession(env, user) {
  const accessToken = base64UrlEncode(randomBytes(32));
  const refreshToken = base64UrlEncode(randomBytes(48));
  const now = Math.floor(Date.now() / 1000);
  await env.DB.prepare(
    `INSERT INTO sessions
      (id, user_id, access_hash, refresh_hash, access_expires_at, refresh_expires_at, created_at)
     VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    crypto.randomUUID(), user.id, await sha256Hex(accessToken), await sha256Hex(refreshToken),
    now + ACCESS_TTL_SECONDS, now + REFRESH_TTL_SECONDS, now
  ).run();

  return {
    userId: user.id,
    email: user.email || "",
    accessToken,
    refreshToken,
  };
}

async function readJson(request) {
  try {
    return await request.json();
  } catch {
    throw new HttpError(400, "Invalid JSON body");
  }
}

async function hashPasswordProof(passwordProof, salt) {
  return sha256Hex(`${salt}:${passwordProof}`);
}

async function sha256Hex(value) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return bytesToHex(new Uint8Array(digest));
}

function randomBytes(length) {
  const bytes = new Uint8Array(length);
  crypto.getRandomValues(bytes);
  return bytes;
}

function bytesToHex(bytes) {
  return Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

function base64UrlEncode(bytes) {
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function constantTimeTextEqual(a, b) {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i += 1) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

function normalizeEmail(value) {
  return String(value || "").trim().toLowerCase();
}

function normalizeHandle(value) {
  return String(value || "").trim().replace(/^@/, "");
}

function splitTags(value) {
  return String(value || "").split(";").map((tag) => tag.trim()).filter(Boolean);
}

function finiteNumberOrNull(value) {
  if (value === null || value === undefined || value === "") return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function json(data, status = 200) {
  return cors(new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  }));
}

function cors(response) {
  const headers = new Headers(response.headers);
  headers.set("access-control-allow-origin", "*");
  headers.set("access-control-allow-methods", "GET,POST,PUT,DELETE,OPTIONS");
  headers.set("access-control-allow-headers", "authorization,content-type");
  return new Response(response.body, { status: response.status, statusText: response.statusText, headers });
}
