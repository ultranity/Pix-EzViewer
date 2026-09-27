# Bookmark state and navigation (#128)

A successful bookmark operation used to update the shared Illust instance, but a later API or navigation snapshot could overwrite that value through CacheRepo.update/copyFrom. The regression suite first reproduced three failures against the old implementation (stale object, stale JSON, and inverse unbookmark overwrite).

The cache now retains server-confirmed local bookmark decisions for the current account session. Unversioned incoming snapshots cannot reverse them. Successful actions update both the callback's instance and any canonical cached instance, so existing screen binders see the result. Details loaded by ID or object resolve to that same canonical instance. Queued UI notifications read the latest value rather than an earlier captured value.

Mutation tokens reject successful callbacks from a previous account session or older than a newer confirmed action. Switching accounts/logout clears cache and decisions; refreshing credentials for the same account does not. No optimistic state is retained after a failed action.

Pixiv responses and navigation payloads do not supply a causal bookmark version. We deliberately do not remove the local protection when a response merely agrees: a still-older response may arrive afterward. The tradeoff is that changes to the same work from another client are picked up after restarting or switching the account session. Cross-device live reconciliation is outside this patch.

Download markers use a different store. The detail view now resets both tint states on binding and refreshes the existing download button on resume, without reloading images or changing download-completion semantics.

Validation: BookmarkStateTest covers the real model/cache/JSON serializer, shared and detached instances, stale bookmark/unbookmark snapshots, failed actions, out-of-order confirmations, account changes, credential refresh and queued UI notifications. Tests use synchronous AndroidX task execution and a test Main dispatcher. These are model-level regression checks; they do not establish real-device navigation acceptance.
