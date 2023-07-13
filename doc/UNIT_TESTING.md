# Unit Testing

BenMAP Cloud uses the JUnit framework for running unit tests. 

## Running Unit Tests
To run unit tests, use `gradle test`.

## Components currently with tests:
- Valuation Function Expressions
- Health Impact Function Expressions

## Components that need unit testing to be implemented:
- Pretty much everything else 

Some code needs to be refactored in order to be more conducive to unit testing. This would
involve breaking down functions into smaller units, and separating out code that accesses
the database from code that runs calculations.
