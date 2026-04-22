# Bottom-Up Parsing Assignment (SLR(1) Part)

## Team
- Member 1: RollNumber1
- Member 2: RollNumber2
- Section: A

## Language
- Java

## Implemented Scope
- Part 1 (SLR(1)):
  - CFG input from file
  - Grammar augmentation
  - Canonical LR(0) item sets (CLOSURE and GOTO)
  - SLR(1) parsing table using FOLLOW sets
  - Conflict detection (shift/reduce and reduce/reduce)
  - Stack-based shift-reduce parsing trace
  - Parse tree generation for accepted strings

## Source Files
- src/Main.java
- src/Grammar.java
- src/SLRParser.java
- src/Items.java
- src/ParsingTable.java
- src/Stack.java
- src/Tree.java

## Compilation
```powershell
javac src\*.java
```

## Execution
```powershell
java -cp src Main <grammar-file> <input-file> [output-dir]
```

Example:
```powershell
java -cp src Main input\grammar2.txt input\input_valid.txt output
```

## Input File Format
- One production per line
- Format: `NonTerminal -> alt1 | alt2 | ...`
- Symbols in RHS must be space-separated
- Epsilon can be written as `epsilon` or `@`

Example:
```text
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

## Input Strings File Format
- One input string per line
- Tokens must be space-separated
- Do not include `$` (added automatically by parser)

Example:
```text
id + id * id
( id + id ) * id
id
```

## Generated Output Files
- output/augmented_grammar.txt
- output/slr_items.txt
- output/slr_parsing_table.txt
- output/slr_trace.txt
- output/parse_trees.txt
- output/slr_summary.txt

## Sample SLR Command
```powershell
java -cp src Main input\grammar2.txt input\input_valid.txt output
```

## Known Limitations
- Input tokenizer expects whitespace-separated tokens.
- Grammar reader assumes one production per line.
- LR(1) parser is intentionally not included in this part (to be implemented by teammate).
