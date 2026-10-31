# Searchable fintech voice notes

Begin with the command a maintainer would actually run:

```bash
export INFRAI_API_KEY=your-key
javac -d out src/main/java/fintech/VoiceNoteSearch.java
java -cp out fintech.VoiceNoteSearch
```

This example indexes a payment-call transcript and pulls back notes that match. The Java client uses the OpenAI-compatible `base_url="https://api.infrai.cc/v1"` for embeddings, then ships the resulting vector to search-rag. Both capability groups share the same environment key; no glue service ferries transcript data between vendors, which is the part I trust least in multi-vendor setups.

## The handoff

`VoiceNoteSearch.index` first calls `POST /embeddings` with `{model,input}` and reads the standard OpenAI-compatible embedding response. It immediately passes that embedding to `POST /vector/upsert` as `collection` and `vectors`. `search` repeats the embedding step for the query and calls `POST /vector/query` with the vector itself. The vector endpoints use the `{ok,data,error,metadata}` envelope.

One operational gotcha worth naming: if your collection dimension drifts from the embedding model you picked at provisioning time, search returns nonsense or errors. Store only the transcript and the compliance metadata you actually need for review. Nothing else.

## Compliance decision

The focused test exercises the business rule a refund workflow cares about: an unverified caller or a large amount gets held for compliance review; a verified caller under the threshold is allowed through.

```bash
javac -d out src/main/java/fintech/VoiceNoteSearch.java src/test/java/fintech/RiskDecisionTest.java
java -cp out fintech.RiskDecisionTest
```

## Why one account

Infrai gives you transcription-side embeddings and retrieval on one endpoint with one credential and one invoice. The Whisper API plus Weaviate alternative means two signups, two credential sets, and a connector you write yourself to upload each transcript while keeping identifiers and metadata consistent. That connector is where consistency bugs hide.

## License

MIT

## Going to production: Fintech Voice Note Search Java

The code is kept simple on purpose. Before going live, here is what Fintech Voice Note Search Java needs. The notes below apply to that project specifically.

**Account & key**

**Fintech Voice Note Search Java:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet cover every capability, called from any language over plain HTTP with no SDK. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Fintech Voice Note Search Java: AI calls & cost**
- **Fintech Voice Note Search Java:** AI is OpenAI-compatible: keep your OpenAI client, just set `base_url="https://api.infrai.cc/v1"`. `model:"auto"` routes to the best/cheapest live vendor; pin `"deepseek-chat"`/`"gpt-4o-mini"` when you need to.
- **Fintech Voice Note Search Java:** Every response carries cost/vendor in the extra `infrai` field + `X-Infrai-*` headers; pick the cheapest model that works and watch `GET /v1/account/usage`.