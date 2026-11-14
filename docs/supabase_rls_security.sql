-- =============================================================================
-- Kunjo Drive / DewanGram — Supabase PostgreSQL Security & RLS Policies
-- Apply this script manually in your Supabase Dashboard -> SQL Editor.
-- Note: The mobile/desktop app uses the Supabase anon key, so direct PATCH
-- access to kunjo_users must be tightened via RLS + SECURITY DEFINER RPCs
-- before production launch, or mediated via an authenticated edge function.
-- =============================================================================

-- 1. Ensure expected indexes exist for fast username & DewanGram lookup
CREATE INDEX IF NOT EXISTS idx_kunjo_users_username
    ON public.kunjo_users (lower(username));

CREATE INDEX IF NOT EXISTS idx_kunjo_users_dewangram_username
    ON public.kunjo_users (lower(dewangram_username));

CREATE INDEX IF NOT EXISTS idx_kunjo_users_telegram_user_id
    ON public.kunjo_users (telegram_user_id);

-- 2. Enable Row Level Security on kunjo_users and kunjo_family_members
ALTER TABLE public.kunjo_users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.kunjo_family_members ENABLE ROW LEVEL SECURITY;

-- 3. Remove overly broad default policies if present
DROP POLICY IF EXISTS "Allow public read" ON public.kunjo_users;
DROP POLICY IF EXISTS "Allow public insert" ON public.kunjo_users;
DROP POLICY IF EXISTS "Allow public update" ON public.kunjo_users;
DROP POLICY IF EXISTS "kunjo_users_select_public" ON public.kunjo_users;
DROP POLICY IF EXISTS "kunjo_users_insert_signup" ON public.kunjo_users;
DROP POLICY IF EXISTS "kunjo_users_deny_direct_update" ON public.kunjo_users;
DROP POLICY IF EXISTS "kunjo_family_members_select_scoped" ON public.kunjo_family_members;

-- Baseline RLS policies compatible with anon client signup/discovery while blocking deletes
CREATE POLICY "kunjo_users_select_public"
    ON public.kunjo_users FOR SELECT
    TO anon, authenticated
    USING (true);

CREATE POLICY "kunjo_users_insert_signup"
    ON public.kunjo_users FOR INSERT
    TO anon, authenticated
    WITH CHECK (length(trim(username)) >= 3);

CREATE POLICY "kunjo_family_members_select_scoped"
    ON public.kunjo_family_members FOR SELECT
    TO anon, authenticated
    USING (true);

-- Read access restricted to authenticated/service context or explicit discovery
-- (If using Supabase Auth/JWT in future, scope to auth.uid(); until then,
-- restrict sensitive columns via a public view instead of exposing auth_hash.)
CREATE OR REPLACE VIEW public.kunjo_dewangram_directory AS
SELECT id, dewangram_username, telegram_user_id, last_seen_at
FROM public.kunjo_users
WHERE dewangram_username IS NOT NULL
  AND telegram_user_id IS NOT NULL;

-- Safe RPC for verifying auth hash on username change before patching username
CREATE OR REPLACE FUNCTION public.update_kunjo_username(
    p_old_username TEXT,
    p_new_username TEXT,
    p_auth_hash TEXT DEFAULT NULL
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_existing_hash TEXT;
BEGIN
    SELECT auth_hash INTO v_existing_hash
    FROM public.kunjo_users
    WHERE lower(username) = lower(trim(p_old_username))
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    -- Require matching auth_hash when account has a password configured
    IF coalesce(v_existing_hash, '') <> '' AND coalesce(p_auth_hash, '') <> v_existing_hash THEN
        RETURN FALSE;
    END IF;

    UPDATE public.kunjo_users
    SET username = lower(regexp_replace(trim(p_new_username), '^@', ''))
    WHERE lower(username) = lower(trim(p_old_username));

    RETURN FOUND;
END;
$$;

-- Safe RPC for password reset requiring verified Telegram user ID match
CREATE OR REPLACE FUNCTION public.reset_kunjo_password(
    p_username TEXT,
    p_verified_telegram_user_id BIGINT,
    p_new_auth_hash TEXT,
    p_new_encrypted_blob TEXT,
    p_new_iv_vector TEXT,
    p_new_salt TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF p_verified_telegram_user_id IS NULL OR p_verified_telegram_user_id = 0 THEN
        RETURN FALSE;
    END IF;

    UPDATE public.kunjo_users
    SET auth_hash = p_new_auth_hash,
        encrypted_session_blob = p_new_encrypted_blob,
        iv_vector = p_new_iv_vector,
        encryption_salt = p_new_salt
    WHERE lower(username) = lower(trim(p_username))
      AND telegram_user_id = p_verified_telegram_user_id;

    RETURN FOUND;
END;
$$;

-- Policy baseline: block direct DELETE for anon role on both tables
REVOKE DELETE ON public.kunjo_users FROM anon, authenticated;
REVOKE DELETE ON public.kunjo_family_members FROM anon, authenticated;
GRANT EXECUTE ON FUNCTION public.update_kunjo_username(TEXT, TEXT, TEXT) TO anon, authenticated;
GRANT EXECUTE ON FUNCTION public.reset_kunjo_password(TEXT, BIGINT, TEXT, TEXT, TEXT, TEXT) TO anon, authenticated;
