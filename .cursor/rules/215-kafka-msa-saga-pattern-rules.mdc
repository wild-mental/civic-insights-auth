---
description: Implements Saga orchestration pattern using Kafka to support distributed transaction rollback.
globs: 
alwaysApply: false
---
Globs: src/main/java/**/*Saga*, src/main/java/**/*Orchestrator*, src/main/resources/**/kafka*, application.yml

# Kafka-based Saga Rules
- Orchestrator should manage event flow and transition states
- Use separate topics per domain action (ex. order-created, payment-done)
- Define clear compensation commands and rollback steps
- Ensure saga ID is propagated across all messages
- Compensating transactions must reverse all previous successful steps
- Design compensators to be idempotent and retry-safe
- Use timeout logic for stalled sagas