create extension if not exists pgcrypto;

create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    handle text not null,
    city text not null default '',
    bio text not null default '',
    avatar_url text,
    created_at timestamptz not null default now()
);

create unique index if not exists profiles_handle_lower_idx on public.profiles (lower(handle));

create table if not exists public.gems (
    id uuid primary key default gen_random_uuid(),
    author_id uuid not null references public.profiles(id) on delete cascade,
    title text not null,
    city text not null,
    neighborhood text not null default '',
    category text not null,
    tip text not null,
    image_url text not null,
    created_at timestamptz not null default now()
);

create table if not exists public.saved_gems (
    user_id uuid not null references public.profiles(id) on delete cascade,
    gem_id uuid not null references public.gems(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, gem_id)
);

create table if not exists public.visited_gems (
    user_id uuid not null references public.profiles(id) on delete cascade,
    gem_id uuid not null references public.gems(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, gem_id)
);

create table if not exists public.follows (
    follower_id uuid not null references public.profiles(id) on delete cascade,
    following_id uuid not null references public.profiles(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (follower_id, following_id),
    constraint no_self_follow check (follower_id <> following_id)
);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
    insert into public.profiles (id, handle, city)
    values (
        new.id,
        coalesce(nullif(new.raw_user_meta_data->>'handle', ''), 'user_' || left(new.id::text, 8)),
        coalesce(new.raw_user_meta_data->>'city', '')
    )
    on conflict (id) do nothing;
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute procedure public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.gems enable row level security;
alter table public.saved_gems enable row level security;
alter table public.visited_gems enable row level security;
alter table public.follows enable row level security;

create policy "profiles readable by everyone"
on public.profiles for select
using (true);

create policy "users update own profile"
on public.profiles for update
using (auth.uid() = id)
with check (auth.uid() = id);

create policy "gems readable by everyone"
on public.gems for select
using (true);

create policy "users create own gems"
on public.gems for insert
with check (auth.uid() = author_id);

create policy "users update own gems"
on public.gems for update
using (auth.uid() = author_id)
with check (auth.uid() = author_id);

create policy "users delete own gems"
on public.gems for delete
using (auth.uid() = author_id);

create policy "saved rows readable by owner"
on public.saved_gems for select
using (auth.uid() = user_id);

create policy "users save gems"
on public.saved_gems for insert
with check (auth.uid() = user_id);

create policy "users unsave gems"
on public.saved_gems for delete
using (auth.uid() = user_id);

create policy "visited rows readable by owner"
on public.visited_gems for select
using (auth.uid() = user_id);

create policy "users mark visited"
on public.visited_gems for insert
with check (auth.uid() = user_id);

create policy "users unmark visited"
on public.visited_gems for delete
using (auth.uid() = user_id);

create policy "follows readable by everyone"
on public.follows for select
using (true);

create policy "users create own follows"
on public.follows for insert
with check (auth.uid() = follower_id);

create policy "users delete own follows"
on public.follows for delete
using (auth.uid() = follower_id);

insert into storage.buckets (id, name, public)
values ('gems', 'gems', true)
on conflict (id) do update set public = true;

create policy "gem images are public"
on storage.objects for select
using (bucket_id = 'gems');

create policy "users upload gem images"
on storage.objects for insert
to authenticated
with check (
    bucket_id = 'gems'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "users update own gem images"
on storage.objects for update
to authenticated
using (
    bucket_id = 'gems'
    and (storage.foldername(name))[1] = auth.uid()::text
)
with check (
    bucket_id = 'gems'
    and (storage.foldername(name))[1] = auth.uid()::text
);

create policy "users delete own gem images"
on storage.objects for delete
to authenticated
using (
    bucket_id = 'gems'
    and (storage.foldername(name))[1] = auth.uid()::text
);
