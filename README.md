# Personal

## fcs-interview-code-assignment

Code assignment built on Quarkus and Java 21, backed by PostgreSQL.

- Answers to the questions: fcs-interview-code-assignment/java-assignment/QUESTIONS.md
- Implementation notes and assumptions: fcs-interview-code-assignment/java-assignment/NOTES.md
- Case study: fcs-interview-code-assignment/case-study/CASE_STUDY.md

The first commit is the assignment exactly as it was provided, so everything after it reads as a diff of the work.

To build and run the tests:

    cd fcs-interview-code-assignment/java-assignment
    ./mvnw verify

Docker is required, the tests start PostgreSQL through Dev Services.
