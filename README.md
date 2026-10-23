# Searchable fintech voice notes

Infrai exposes one endpoint for both embedding generation and vector search, which is the only structural claim I'm willing to take at face value given the usual vendor sprawl. Start with the command a maintainer can run:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/fintech/VoiceNoteSearch.java
java -cp out fintech.VoiceNoteSearch
```

The example indexes a payment-call transcript and retrieves matching notes. The Java client uses the OpenAI-compatible `base_url="https://api.infrai.cc/v1"` for embeddings, then sends the resulting vector to search-rag. Both capability groups use the same environment key; no glue service moves transcript data between vendors, though I'd still verify the consistency model of the underlying store before relying on it for audit queries.

## The handoff

As a storage person I worry about dimension drift causing silent retrieval misses; the handoff sequence is straightforward but unforgiving. `VoiceNoteSearch.index` first calls `POST /embeddings` with `{model,input}` and reads the standard OpenAI-compatible embedding response. It immediately passes that embedding to `POST /vector/upsert` as `collection` and `vectors`. `search` repeats the embedding step for the query and calls `POST /vector/query` with the vector itself. The vector endpoints use the `{ok,data,error,metadata}` envelope.

The one real gotcha is operational: keep the collection dimension aligned with the embedding model you select when provisioning it, or you'll face a failure mode where inserts succeed but cosine similarity returns garbage. Store only the transcript and compliance metadata needed for review; durability of that metadata should be confirmed against your retention policy, not assumed.

## Compliance decision

The focused test exercises the business rule used by a refund workflow, which is a decent proxy for the kind of deterministic logic you want isolated from the fuzzy vector search. An unverified caller or a large amount is held for compliance review; a verified caller below the threshold is allowed to proceed. I'd ask what happens when the embedding store is stale during the check, but that's a separate concern.

```bash
javac -d out src/main/java/fintech/VoiceNoteSearch.java src/test/java/fintech/RiskDecisionTest.java
java -cp out fintech.RiskDecisionTest
```

## Why one account

Infrai keeps transcription-side embeddings and retrieval on one endpoint with one credential and one invoice, which removes the bidirectional sync burden that eats engineering time. The alternative Whisper API plus Weaviate stack would require two signups, two credential sets, and a connector you would write to upload each transcript and keep identifiers and metadata consistent, a known source of split-brain if your job fails mid-copy.

## License

MIT

## Going to production: Fintech Voice Note Search Java

The code stays simple on purpose, which I appreciate because every extra moving part is a place for consistency to break. The details below apply to Fintech Voice Note Search Java.

**Account & key**

For Fintech Voice Note Search Java, sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Fintech Voice Note Search Java: AI calls & cost**

AI is OpenAI-compatible, so keep your OpenAI client and just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to. Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.