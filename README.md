# Tasks API

Example **Spring Boot** REST service for managing tasks. Data is kept **in memory** (it is lost when the application stops). The API is described with **OpenAPI 3** via [springdoc-openapi](https://springdoc.org/) and exposed in **Swagger UI**.

The project also includes a **HAL hypermedia** surface (Spring HATEOAS): paginated collections and per-resource `_links` so clients can discover related URLs instead of hard-coding paths.

## Requirements

- **Java 17** or newer  
- **Maven 3.6+**

## Run the application

```bash
mvn spring-boot:run
```

Default base URL: `http://localhost:8080`

## API overview

### JSON CRUD (`/api/tasks`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/tasks` | List all tasks |
| `GET` | `/api/tasks/{id}` | Get one task by UUID |
| `POST` | `/api/tasks` | Create a task (`title` required) |
| `PUT` | `/api/tasks/{id}` | Replace a task (`title`, `completed` required) |
| `PATCH` | `/api/tasks/{id}` | Partial update (only sent fields change) |
| `DELETE` | `/api/tasks/{id}` | Delete a task (returns `204 No Content`) |

### Hypermedia and pagination (`/api/hypermedia/tasks`)

These endpoints return **HAL** JSON (`application/hal+json` by default): task fields appear next to `_links`, and paged lists use `_embedded.tasks`, `page`, and navigation links.

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/hypermedia/tasks` | Paginated list. Query: `page` (default `0`), `size` (default `10`, max `100`). `_links` include `self`, `first`, `last`, and `next` / `prev` when applicable. |
| `GET` | `/api/hypermedia/tasks/{id}` | One task with `_links.self`, `_links.collection` (first page of the hypermedia list), and `_links.alternate` (plain JSON at `/api/tasks/{id}`). |

Tasks are ordered by `createdAt`, then `title`, so paging is stable for the in-memory store.

### Task JSON shape

| Field | Type | Notes |
|-------|------|--------|
| `id` | UUID | Set by the server on create |
| `title` | string | Required on create / full update |
| `description` | string | Optional |
| `completed` | boolean | Default `false` on create |
| `createdAt` | instant (ISO-8601) | Read-only |
| `updatedAt` | instant (ISO-8601) | Read-only |

### Example: create and fetch

```bash
curl -s -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","description":"2%","completed":false}'
```

Use the returned `id` in:

```bash
curl -s http://localhost:8080/api/tasks/<id>
```

### Example: paged HAL collection

```bash
curl -s "http://localhost:8080/api/hypermedia/tasks?page=0&size=5"
```

### Example: HAL item (discover related URLs from `_links`)

```bash
curl -s "http://localhost:8080/api/hypermedia/tasks/<id>"
```

## Postman

Import the collection (variables: `baseUrl`, `taskId`, `page`, `pageSize`):

**[`postman/Tasks-API.postman_collection.json`](postman/Tasks-API.postman_collection.json)**

1. Start the app: `mvn spring-boot:run`  
2. Run **Create task** — it stores the returned `id` in `taskId`.  
3. Use **Tasks (hypermedia)** for paginated HAL and item links, or adjust `page` / `pageSize` on the list request.

## OpenAPI and Swagger UI

| Resource | URL |
|----------|-----|
| OpenAPI JSON | [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs) |
| Swagger UI | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) |

## Tests

The project includes **unit**, **integration** (MockMvc), and **API** (embedded server + `TestRestTemplate`) tests.

```bash
mvn test
```

- **Unit:** `TaskService` logic without Spring (`com.example.tasks.unit`).  
- **Integration:** full servlet stack with MockMvc (`com.example.tasks.integration`), including hypermedia tests.  
- **API:** HTTP calls on a random port (`com.example.tasks.api`).

## Project layout

```
src/main/java/com/example/tasks/
  TasksApplication.java       # Entry point
  config/OpenApiConfig.java   # API title / description in the spec
  dto/                        # Request bodies for POST, PUT, PATCH
  hypermedia/                 # HAL controllers and assemblers (pagination + _links)
  model/Task.java             # Task resource
  service/TaskService.java    # In-memory store + paged reads
  web/                        # REST controller and exception handling
src/main/resources/
  application.properties      # App name and springdoc paths
postman/
  Tasks-API.postman_collection.json
src/test/java/                # Unit, integration, and API tests
```

## Build artifact

```bash
mvn -q package
java -jar target/tasks-api-1.0.0-SNAPSHOT.jar
```
