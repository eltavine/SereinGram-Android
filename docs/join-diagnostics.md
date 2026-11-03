# Joining diagnostics

`CHANNELS_TOO_MUCH` is a Telegram server response. SereinGram cannot remove
it by increasing a local limit or reporting a failed join as successful.
The client now binds invitations to the request account and offers accurate
diagnostics instead of asserting that the user has reached 500 memberships.

## Default API mismatch

SereinGram previously hardcoded API ID `11535358` and described it as Nagram's
default. Inspection of Nagram's [13.0.2 / 1255 arm64 release](https://github.com/NextAlone/Nagram/releases/tag/1255)
confirmed that its default is actually **`896686`**, with a different paired
API hash. This change aligns both default values with that released package;
it retains the existing explicit custom-API and build overrides.

The reference APK SHA-256 is
`e6a9aad27dedf774f9ab3a19fd12a5800b5d0105619d2e5f6a299228aaba5a2c`,
matching the release asset's digest. Static inspection traced
`ConnectionsManager.<init>` to the default API selector and its
`org.telegram.messenger.BuildConfig` constants. A source tree whose default
is `0` is insufficient evidence of the credentials injected into a release.

Device testing confirmed that updating an existing installation left its
server authorization associated with `11535358`, even though the local
configuration had changed to `896686`. The reporter subsequently confirmed
normal functionality after testing a fresh login in a separate installation
of the same code. This supports the fix for the reported case, without
establishing Telegram's internal restriction policy.

## Existing installations

Changing the APK's default credentials does not reauthorize an existing
session. If the diagnostic shows local API `896686` but server session API
`11535358`, a fresh login is needed to test the corrected credentials.
Importing a saved session reuses its old authorization key and is not a
fresh login. User-selected custom API settings keep their existing priority.

A separate test installation can establish a fresh login while keeping the
original installation's data. For an existing installation, users can sign
out and sign in normally after preserving needed local data and confirming
they can complete authentication. Telegram secret chats do not survive
logout; cloud chats can synchronize again. This change does not automatically
log users out, copy session keys, or erase local data.

## Interpreting a report

The membership check reads fresh server dialogs, including archived dialogs,
and counts distinct joined supergroups/channels. It labels missing data or
unfinished pagination as incomplete. This is a count of the server's dialog
list, not access to Telegram's internal quota or anti-abuse decisions.

The locally configured API ID includes the login screen's custom API setting.
The current session API ID is read separately from `account.getAuthorizations`,
using only the authorization marked `current`. A missing, failed or ambiguous
response is reported as unverified. Session hashes, IP addresses and other
devices are neither copied into the report nor persisted by this diagnostic.

The initial test package displayed the build's default API ID. That value
did not prove which API the current session used. Installing an APK with a
different default also does not prove an existing authorization has changed.

A low complete dialog count and successful joining in official Telegram with
the same account make a simple 500-membership explanation inadequate. They do
not prove that a particular API ID is restricted. The similar
[TDLib report #3001](https://github.com/tdlib/td/issues/3001) concerns a different
API ID and is not a diagnosis of SereinGram's shared ID.

## Compare while keeping Nagram's credentials

This change uses the verified Nagram release's default credentials. It does
not automatically change a user-selected custom API or log anyone out.
Use the same account in official Telegram, the original Nagram and SereinGram
to narrow down the difference:

| Observed result | What to investigate next |
| --- | --- |
| Official Telegram succeeds; both Nagram and SereinGram fail | A shared API or third-party-session restriction is plausible, but not proven. |
| Official Telegram and Nagram succeed; SereinGram fails | SereinGram's session and client differences deserve further investigation; the API ID alone is not an explanation. |
| All three fail | Recheck the target, membership, account state and server error. |

Use a target the account has not already joined. Once the official-client
test succeeds, the account is a member in every client, so opening that group
in SereinGram is not another successful join test. If you choose to leave and
repeat the test, allow membership state to synchronize before trying again.
Test public links and private invitations separately. An approval-required
invitation should return a pending request, not immediate membership.

The diagnostic can be run from the join-error dialog. Copy its result with
the configured API ID, server session API ID, and complete/partial count.
Record the client comparison separately. Keep private invitation links out
of public reports. Do not uninstall or clear app data merely to obtain a
diagnostic.

## Current boundary

The request path sends `channels.joinChannel` or `messages.importChatInvite`
on the selected request account. The inspected client code does not synthesize
`CHANNELS_TOO_MUCH` from a local count. This version corrects the default
credential mismatch and improves the diagnostic. The reported device case
passed after a fresh login; other accounts or server restrictions still need
their own evidence, and existing sessions are not silently migrated.

Opening the same link in official Telegram is an explicit fallback. Once
joined there, the membership can sync to SereinGram. It is not proof that
SereinGram's join request succeeded. If an API-level restriction is confirmed,
its owner and Telegram may need to resolve it. Buying Premium is not a
confirmed remedy for the reported case.
