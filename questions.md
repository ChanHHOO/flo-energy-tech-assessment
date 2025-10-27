> What is the rationale for the technologies you have decided to use?

  The main technologies used and their rationales are as
  follows:

  1. **Kotlin**:
  I noticed in the job description that Flo uses Kotlin. I
  chose Kotlin because I believed it would be the most
  effective way for the Flo team to understand my design
  and implementation approach. <br> To be honest, I didn't specifically focus on leveraging JVM
   advantages (such as garbage collection or multithreading)
  in this project, nor did I apply those features. My
  priority was object-oriented design principles,
  particularly separation of business concerns, rather than
  platform-specific optimizations.

  2. **SQLite**:
  I chose SQLite because it's well-suited for
  assignment-style projects due to its simple setup with no
  external dependencies. During the initial development
  phase, database schemas often change frequently. I thought
  it would be practical to validate the implementation with
  SQLite first, then migrate to a production database like
  MySQL or PostgreSQL later.

> What would you have done differently if you had more time?

1. I would have implemented a better dependency management
  approach. Currently, the main function depends on all
  layers: service, repository, and handler. This violates the
   Layered Architecture principle, but it was unavoidable
  since I didn't use Spring Framework and therefore had no
  access to a Bean Container for dependency injection. <br> 
  With more time, I would have implemented a proper DI
  container (similar to Spring's Bean Container) to manage
  dependencies correctly and maintain cleaner separation
  between layers.

  2. I missed the opportunity to set up a CI pipeline step
  that would automatically run tests and block merges of failing code
  into the main branch.

> What is the rationale for the design choices that you have made?

  I prioritized code readability and maintainability for
  future developers.

  Initially, I started without a strict Layered Architecture,
   just separating some concerns. As development progressed,
  this approach made it difficult to define clear dependency
  relationships, and each component's responsibility became
  ambiguous. This ultimately made writing test code
  challenging and lowered overall code quality.

  To address these issues, I refactored the code using proper
   design patterns (Repository, Composite, Template Method,
  etc.) and established a clear Layered Architecture(in #12 PR). The
  detailed rationale for each design decision is documented
  in the README.md file.
