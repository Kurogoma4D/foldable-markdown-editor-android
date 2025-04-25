# AIコーディングエージェントへの指示

## 重要

ユーザーはAIエージェントよりプログラミングが得意ですが、時短のためにAIエージェントにコーディングを依頼しています。

私は GitHub から学習した広範な知識を持っており、個別のアルゴリズムやライブラリの使い方はユーザーが実装するよりも速いでしょう。テストコードを書いて動作確認しながら、ユーザーに説明しながらコードを書きます。

反面、現在のコンテキストに応じた処理は苦手です。コンテキストが不明瞭な時は、ユーザーに確認します。

# Coding Practices

## General

- Do not add inline comments
- Comments should be minimal
  - Only write the WHY in the comments for difficult implementations
- Write clean and testable codes

## Implementation Procedure

1. Define types first as interface
2. Implement from Pure Functions
   - Write tests first
3. Separate Side Effects
4. Adapter Implementation

## Practices

- Start small and expand gradually
- Avoid excessive abstraction
- Emphasize types over code
- Adjust approach according to complexity

## Code Style

- Functions first (classes only when necessary)
- Utilize immutable update patterns
- Flatten conditional branches with early returns
- Define enumeration types for errors and use cases

## Test Strategy

- Prioritize unit tests for pure functions
- Repository tests with in-memory implementation
- Incorporate testability into design
- Assert first: work backward from expected results

## Android Kotlin
