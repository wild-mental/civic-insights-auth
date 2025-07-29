---
description: Defines topic and message structuring rules for Kafka-based data pipelines.
globs: 
alwaysApply: false
---
Globs: src/main/java/**/*Kafka*, src/main/resources/**/kafka*, docker-compose.yml

# Kafka Topic & Partition Strategy
- Name topics by business domain, with semantic clarity
- Use compacted topics for latest state tracking
- Use key-based partitioning if order matters (e.g. per user, orderId)
- Use random UUID key only if message order doesn't matter
- Consumers must implement idempotency
- Acknowledge only after successful downstream operation
- Consider dead-letter topics for poison messages