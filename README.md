# SLR(1) and LR(1) Parser

## Team
- Member 1: RollNumber1
- Member 2: RollNumber2
- Section: A

## Language
Java (JDK 11+)

## Source Files
```
src/
  Main.java          - Driver: runs both parsers, prints output, writes files
  Grammar.java       - CFG loading, augmentation, FIRST/FOLLOW sets
  SLRParser.java     - SLR(1) canonical collection, parsing table, parser
  LR1Parser.java     - LR(1) canonical collection, parsing table, parser
  Items.java         - Production, LR0Item, LR1Item, Action, ParsingStep
  ParsingTable.java  - ACTION/GOTO table with conflict detection
  Stack.java         - Generic stack
  Tree.java          - Parse tree node with ASCII rendering
```

## Compilation
From the project root:
```
javac src\*.java
```

## Running
```
java -cp src Main <grammar-file> <input-file> [output-dir]
```

All output is printed to the terminal AND written to files in `output-dir`.

### Examples

**Grammar 2 (full expression grammar — no conflicts):**
```
java -cp src Main input\grammar2.txt input\input_valid.txt output
```

**Grammar 3 (LR(1) superiority demo — SLR has conflict, LR(1) does not):**
```
java -cp src Main input\grammar3.txt input\input_valid.txt output
```

**Invalid inputs (error handling):**
```
java -cp src Main input\grammar2.txt input\input_invalid.txt output
```

**Dangling else (conflict in both parsers — ambiguous grammar):**
```
java -cp src Main input\grammar_with_conflict.txt input\input_valid.txt output
```

## Input Grammar Format
One production per line. Alternatives separated by `|`.
Non-terminals must start with an uppercase letter and be multi-character.
Epsilon: use `epsilon` or `@`.
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

## Input Strings Format
One string per line, tokens space-separated. `$` is added automatically.
Lines starting with `#` are comments and are skipped.
A blank line tests the empty-input case.
```
id + id * id
( id + id ) * id
id
```

## Output Files (written to output/)
| File | Contents |
|------|----------|
| `augmented_grammar.txt` | Grammar with augmented start symbol |
| `slr_items.txt` | All SLR(1) LR(0) item sets with transitions |
| `slr_parsing_table.txt` | SLR(1) ACTION/GOTO table + conflicts |
| `slr_trace.txt` | Step-by-step SLR(1) parsing trace |
| `parse_trees.txt` | SLR(1) parse trees + reduction steps |
| `slr_summary.txt` | SLR(1) statistics |
| `lr1_items.txt` | All LR(1) item sets with lookaheads |
| `lr1_parsing_table.txt` | LR(1) ACTION/GOTO table + conflicts |
| `lr1_trace.txt` | Step-by-step LR(1) parsing trace |
| `lr1_parse_trees.txt` | LR(1) parse trees + reduction steps |
| `lr1_summary.txt` | LR(1) statistics |
| `comparison.txt` | Side-by-side comparison (states, table size, time, conflicts) |

## Known Limitations
- Tokens must be whitespace-separated (no character-level lexer).
- Grammar non-terminals must be multi-character starting with uppercase.
- Single-character non-terminals (E, T, F) are not supported.
