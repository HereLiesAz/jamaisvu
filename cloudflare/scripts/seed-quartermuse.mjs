import fs from 'node:fs';
import crypto from 'node:crypto';

const input = process.argv[2] || new URL('../seed/quartermuse_master_v11.csv', import.meta.url);
const text = fs.readFileSync(input, 'utf8').replace(/^\uFEFF/, '');
const rows = parseCsv(text);
const header = rows.shift();
const expected = ['Venue', 'Latitude', 'Longitude', 'Category Tags'];
if (JSON.stringify(header) !== JSON.stringify(expected)) {
  throw new Error(`Unexpected CSV header: ${JSON.stringify(header)}`);
}

console.log('-- Generated from cloudflare/seed/quartermuse_master_v11.csv');
console.log("INSERT OR IGNORE INTO users (id, handle, city, bio, created_at) VALUES ('seed-quartermuse', 'quartermuse', 'New Orleans', 'Curated New Orleans place catalog.', unixepoch());");

rows.filter((row) => row.some((cell) => cell.trim() !== '')).forEach((row, index) => {
  if (row.length !== 4) throw new Error(`Row ${index + 2} has ${row.length} columns`);
  const [venue, latText, lonText, tags] = row.map((value) => value.trim());
  const latitude = Number(latText);
  const longitude = Number(lonText);
  if (!venue || !Number.isFinite(latitude) || !Number.isFinite(longitude) || !tags) {
    throw new Error(`Invalid data on row ${index + 2}`);
  }
  const digest = crypto.createHash('sha1').update(`${venue}|${latText}|${lonText}`).digest('hex').slice(0, 10);
  const id = `qm-${String(index + 1).padStart(3, '0')}-${digest}`;
  const category = deriveCategory(tags);
  console.log(
    `INSERT OR IGNORE INTO gems (id, author_id, title, city, neighborhood, category, tags, tip, image_url, latitude, longitude, source, created_at, updated_at) VALUES (` +
    `'${sql(id)}', 'seed-quartermuse', '${sql(venue)}', 'New Orleans', '', '${sql(category)}', '${sql(tags)}', '', NULL, ${latitude}, ${longitude}, 'quartermuse_master_v11', unixepoch(), unixepoch());`
  );
});

function deriveCategory(rawTags) {
  const tags = new Set(rawTags.split(';').map((tag) => tag.trim()));
  if (hasAny(tags, ['Craft Cocktails', 'Dive Bar', 'Historic Bar', 'Dive Bar Adjacent', 'Cocktail History'])) return 'Drink';
  if (hasAny(tags, ['Seafood', 'Historic Restaurant', 'Fine Dining', 'Casual Dining', 'Cheap Eats', 'Late Night Food', 'Craft Coffee'])) return 'Eat';
  if (tags.has('Shopping')) return 'Shop';
  if (hasAny(tags, ['Occult', 'Haunted', 'Goth', 'Gothic', 'Dark Tourism', 'Witchcraft', 'Weird', 'Voodoo Culture'])) return 'Weird';
  if (hasAny(tags, ['Live Music', 'Jazz Essential', 'Punk', 'Late Night', 'Open After 2AM', 'Alternative', 'Underground', 'LGBTQ+'])) return 'Play';
  return 'Art';
}

function hasAny(set, values) {
  return values.some((value) => set.has(value));
}

function sql(value) {
  return String(value).replaceAll("'", "''");
}

function parseCsv(source) {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < source.length; i += 1) {
    const char = source[i];
    if (quoted) {
      if (char === '"' && source[i + 1] === '"') {
        field += '"';
        i += 1;
      } else if (char === '"') {
        quoted = false;
      } else {
        field += char;
      }
    } else if (char === '"') {
      quoted = true;
    } else if (char === ',') {
      row.push(field);
      field = '';
    } else if (char === '\n') {
      row.push(field.replace(/\r$/, ''));
      rows.push(row);
      row = [];
      field = '';
    } else {
      field += char;
    }
  }
  if (quoted) throw new Error('Unterminated quoted field');
  if (field.length || row.length) {
    row.push(field.replace(/\r$/, ''));
    rows.push(row);
  }
  return rows;
}
