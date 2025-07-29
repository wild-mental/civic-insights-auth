---
description: Chooses Redis client strategy (Lettuce vs Redisson) based on caching or distributed requirements.
globs: 
alwaysApply: false
---
Globs: src/main/java/**/*Redis*, src/main/resources/**/redis*, application.yml

# Redis Client Selection Rules
- Use Lettuce if:
  - Simple caching use case
  - Basic key-value read/write
  - Lightweight dependency preferred
  - Spring Data Redis default suffices
- Use Redisson if:
  - Distributed lock required
  - Shared distributed objects needed
  - More complex cluster coordination needed
  - Java collection-like APIs preferred
- Do not mix Lettuce and Redisson for same cache purpose