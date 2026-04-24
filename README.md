# CS4031 – Compiler Construction
## Assignment 03: Bottom-Up Parser (SLR(1) and LR(1))

---

## Team Members

M.Faheem, 23i-0728
Humayun Bilal, 23i-0832

**Section:** D

---

## Programming Language

**Java** — JDK 11 or higher required.

---

## Project Structure

```
SLR1-LR1-Parser/
├── src/
│   ├── Main.java           Driver: orchestrates both parsers, prints & saves output
│   ├── Grammar.java        CFG file reader, augmentation, FIRST and FOLLOW sets
│   ├── SLRParser.java      SLR(1): LR(0) canonical collection, table, shift-reduce parser
│   ├── LR1Parser.java      LR(1): LR(1) canonical collection, table, shift-reduce parser
│   ├── Items.java          Data types: Production, LR0Item, LR1Item, Action, ParsingStep
│   ├── ParsingTable.java   ACTION/GOTO table storage with conflict detection
│   ├── Stack.java          Generic stack wrapper
│   └── Tree.java           Parse tree node with ASCII pretty-printer
├── input/
│   ├── grammar1.txt        Simple expression grammar (id, +)
│   ├── grammar2.txt        Full expression grammar (id, +, *, parentheses)
│   ├── grammar3.txt        Classic LR(1)-but-NOT-SLR(1) grammar (L=R example)
│   ├── grammar_with_conflict.txt   Dangling-else grammar (ambiguous, conflicts in both)
│   ├── input_valid.txt     Valid input strings for expression grammars
│   └── input_invalid.txt   Invalid input strings (syntax errors, empty input)
├── output/
│   └── <grammar-name>/     One subfolder per grammar run (auto-created)
│       ├── augmented_grammar.txt
│       ├── slr_items.txt
│       ├── slr_parsing_table.txt
│       ├── slr_trace.txt
│       ├── lr1_items.txt
│       ├── lr1_parsing_table.txt
│       ├── lr1_trace.txt
│       ├── comparison.txt
│       └── parse_trees.txt
├── build.ps1               PowerShell build + run script
└── README.md
```

---

## Compilation

Open a terminal in the project root and run:

```
javac src\*.java
```

This compiles all `.java` files into `src\`. No external libraries are required.

---

## Execution

### Command syntax

```
java -cp src Main <grammar-file> <input-file> [base-output-dir]
```

| Argument | Description |
|----------|-------------|
| `<grammar-file>` | Path to the grammar `.txt` file |
| `<input-file>` | Path to the input strings `.txt` file |
| `[base-output-dir]` | *(Optional)* Base output directory. Defaults to `output`. A subfolder named after the grammar file is created automatically inside it. |

All output is **printed to the terminal** and **saved to files** simultaneously.

---

### Sample commands

**Run SLR(1) and LR(1) on the full expression grammar (grammar2):**
```
java -cp src Main input\grammar2.txt input\input_valid.txt output
```
Output saved to: `output\grammar2\`

**Run SLR(1) and LR(1) on the LR(1) superiority demo (grammar3 — SLR has conflict, LR(1) does not):**
```
java -cp src Main input\grammar3.txt input\input_valid.txt output
```
Output saved to: `output\grammar3\`

**Run on the dangling-else grammar (conflict in both parsers — ambiguous):**
```
java -cp src Main input\grammar_with_conflict.txt input\input_valid.txt output
```
Output saved to: `output\grammar_with_conflict\`

**Test error handling with invalid input strings:**
```
java -cp src Main input\grammar2.txt input\input_invalid.txt output
```

**Specify a custom output directory:**
```
java -cp src Main input\grammar2.txt input\input_valid.txt my_output
```
Output saved to: `my_output\grammar2\`

---

### Using the build script (PowerShell)

```powershell
.\build.ps1                                              # default: grammar2 + input_valid
.\build.ps1 input\grammar3.txt                          # custom grammar, default input
.\build.ps1 input\grammar3.txt input\input_valid.txt    # custom grammar + input
.\build.ps1 input\grammar3.txt input\input_valid.txt my_output  # custom output dir
```

---

## Grammar File Format

- One production rule per line
- Format: `NonTerminal -> alternative1 | alternative2 | ...`
- Use `->` as the arrow symbol
- Separate RHS symbols with spaces
- **Non-terminals:** multi-character names starting with an uppercase letter (e.g., `Expr`, `Term`, `Factor`)
- **Single-character non-terminals are NOT allowed** (e.g., `E`, `T`, `F` are invalid)
- **Terminals:** lowercase letters, operators, keywords (e.g., `id`, `+`, `*`, `if`, `then`)
- **Epsilon:** write `epsilon` or `@`
- Lines starting with `#` are treated as comments and ignored

**Example (`grammar2.txt`):**
```
Expr -> Expr + Term | Term
Term -> Term * Factor | Factor
Factor -> ( Expr ) | id
```

**Epsilon production example:**
```
Stmts -> Stmt Stmts | epsilon
```

---

## Input Strings File Format

- One input string per line
- Tokens must be **space-separated**
- The end marker `$` is added automatically — do not include it
- Lines starting with `#` are comments and are ignored
- A **blank line** is treated as an empty-input test case

**Example (`input_valid.txt`):**
```
id + id * id
( id + id ) * id
id
```

**Example (`input_invalid.txt`):**
```
+ id
( id +
id id
```

---

## Output Files

Each run creates a subfolder `output/<grammar-name>/` containing exactly these files:

| File | Contents |
|------|----------|
| `augmented_grammar.txt` | Augmented grammar with new start symbol (e.g., `ExprPrime -> Expr`) |
| `slr_items.txt` | All SLR(1) LR(0) item sets (states I0, I1, …) with GOTO transitions |
| `slr_parsing_table.txt` | SLR(1) ACTION/GOTO table; lists any conflicts found |
| `slr_trace.txt` | Step-by-step SLR(1) parsing trace for every input string |
| `lr1_items.txt` | All LR(1) item sets with lookaheads `[A -> α . β, a]` |
| `lr1_parsing_table.txt` | LR(1) ACTION/GOTO table; lists any conflicts found |
| `lr1_trace.txt` | Step-by-step LR(1) parsing trace for every input string |
| `parse_trees.txt` | Parse trees (SLR then LR(1)) with numbered reduction steps for each accepted string |
| `comparison.txt` | Side-by-side comparison: states, table size, build time, parse time, conflicts, and conflict analysis |

---

## How SLR(1) and LR(1) Differ

| Aspect | SLR(1) | LR(1) |
|--------|--------|-------|
| Items used | LR(0) items | LR(1) items with lookahead |
| Reduce condition | All terminals in `FOLLOW(A)` | Only the specific lookahead `a` in `[A -> α ., a]` |
| States generated | Fewer | More (each state tracks exact lookaheads) |
| Parsing power | Weaker | Strictly stronger |
| Conflict resolution | Cannot resolve some conflicts | Resolves conflicts SLR(1) cannot |

---

## Known Limitations

- Tokens must be whitespace-separated; there is no character-level lexer.
- Grammar non-terminals must be multi-character and start with an uppercase letter.
- Single-character non-terminals (`E`, `T`, `F`, etc.) are not supported.
- Ambiguous grammars (e.g., dangling-else) will have conflicts in both parsers; this is expected and reported.
- On conflict, the first-encountered action is kept and the conflict is logged — the parser does not crash.
