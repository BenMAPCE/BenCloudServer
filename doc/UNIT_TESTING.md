# Unit Testing

BenMAP Cloud uses the JUnit framework for running unit tests. 

## Running Unit Tests
To run unit tests, use `gradle test`.

## Components currently with tests:
- Valuation Function Expressions & Health Impact Function Expressions
- HIFConfig, ScenarioHIFConfis, ScenarioPopConfig, and Valuation Config constructors


## Components that need unit testing to be implemented:
- ValuationTaskConfig & HIFTaskConfig constructors
    - These constructors take raw JSON strings rather than JsonNode classes as an argument, and so will throw if invalid json is passed to it. 
    When writing tests for these, we might want to make these classes behave like other config classes.
- Pretty much everything else 

Some code needs to be refactored in order to be more conducive to unit testing. This would
involve breaking down functions into smaller units, and separating out code that accesses
the database from code that runs calculations.

