# Smart Campus REST API Documentation

## Overview of the API Design

This project implements a Smart Campus REST API using JAX-RS (Jersey) and in-memory data structures (HashMap, ArrayList).

The API exposes resources for:
* **Discovery:** `GET /api/v1` with metadata and resource map
* **Rooms:** CRUD-style operations for campus rooms
* **Sensors:** Registration and filtering of sensors linked to rooms
* **Readings:** Nested sub-resources for sensor readings with historical logs

### Design Features
* Sub-resource locators for `/sensors/{sensorId}/readings`
* Custom exceptions + `ExceptionMappers` for 409, 422, 403, 500
* JAX-RS filters for request/response logging
* HATEOAS-style links in the discovery endpoint

---

## Build and Run Instructions

### Prerequisites
* Java 17 (or your module’s required version)
* Maven installed and on your PATH

### Setup
1.  **Clone the repository**
    ```bash
    git clone [https://github.com/SoheibKaddouri/smart-campus-api.git](https://github.com/SoheibKaddouri/smart-campus-api.git)
    cd smart-campus-api
    ```
2.  **Build the project**
    ```bash
    mvn clean install
    ```
3.  **Run the server**
    ```bash
    mvn exec:java -Dexec.mainClass="com.smartcampus.server.ServerLauncher"
    ```

### Base URL
The API will be available at: `http://localhost:8080/api/v1`

---

## Sample curl commands
*Adjust IDs to match what you create in your own tests.*

### 1. Discovery endpoint
```bash
curl -X GET http://localhost:8080/api/v1
```
### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "id": "LIB-301",
    "name": "Library Study Room 301",
    "capacity": 20
  }'
```
### 3. Create a Sensor (Valid Room ID)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-001",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 21.5,
    "roomId": "LIB-301"
  }'
```
### 4. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```
### 5. Add a Reading to a Sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{
    "id": "READ-001",
    "timestamp": "2025-04-23T10:00:00Z",
    "value": 22.3
  }'
```
### 6. Trigger 422 (Invalid Room ID for Sensor)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{
    "id": "TEMP-999",
    "type": "Temperature",
    "status": "ACTIVE",
    "currentValue": 19.0,
    "roomId": "NON_EXISTENT_ROOM"
  }'
```

## Report Answers
# Part 1: JAX‑RS Resource Lifecycle

By default, JAX‑RS resource classes use a **per‑request** lifecycle.

### This means:
* A new instance of the resource class is created for every incoming HTTP request.
* After the request is processed, the instance becomes eligible for garbage collection.
* The resource class is not treated as a singleton unless explicitly annotated with `@Singleton`.

---

## Impact on In‑Memory Data Structures

Because each request receives a fresh instance:
* **Instance fields are NOT shared between requests**
    * → You cannot store application data inside resource class fields.
* **Shared data must be stored in static structures**
    * *e.g.*, `private static Map<String, Room> rooms = new HashMap<>();`

### Concurrency Concerns
Since multiple requests may access these structures at the same time, you must consider:
* Race conditions
* Concurrent modifications
* Thread safety

### How to avoid data loss or corruption
* **Use thread‑safe collections** (e.g., `ConcurrentHashMap`)
* **Or synchronize access manually**
* **Or ensure operations are atomic** (e.g., check‑then‑insert)

---

## Why this lifecycle is beneficial
* It avoids shared mutable state inside resource objects
* It keeps resource classes stateless and safe
* It scales well under high load



# Why Hypermedia (HATEOAS) Matters in REST APIs

**HATEOAS** (Hypermedia As The Engine Of Application State) means that a REST API includes links inside its responses that show clients how to navigate the system.

Instead of relying only on external documentation, the API becomes **self‑describing**.

---

### Why this is considered advanced REST design
* **Actionable Responses:** The API tells clients what actions are possible next.
* **Dynamic Discovery:** Clients discover available resources dynamically.
* **Reduced Coupling:** It reduces coupling between client and server.
* **Pure REST:** It aligns with the original REST constraints defined by Roy Fielding.

---

### Benefits for client developers

#### 1. Self‑navigation without documentation
Clients can follow links like `"rooms": "/api/v1/rooms"` directly from the API response instead of reading a PDF or wiki.

#### 2. Automatic adaptability
If the server changes a URL, clients still work because they follow the links provided at runtime.

#### 3. Reduced errors
Clients don’t hard‑code paths, so they avoid broken URLs.

#### 4. Better developer experience
The API behaves more like a web browser:
> → You don’t need to know every URL in advance; you just follow links.

---

### In summary
HATEOAS transforms a REST API from a static interface into a discoverable, self‑documenting system, making client development easier, safer, and more resilient to change.



# Part 2

## Returning IDs vs Full Room Objects

When returning a list of rooms, the API designer must choose between:

### Option 1 — Return only room IDs

**Example:**

```json
["LIB-301", "ENG-204", "SCI-110"]
```

### Option 2 — Return full room objects

**Example:**

```json
[
  { "id": "LIB-301", "name": "Library Quiet Study", "capacity": 40 },
  { "id": "ENG-204", "name": "Engineering Lab", "capacity": 25 }
]
```

## Implications of returning only IDs

### Advantages
- Much smaller payload → lower network bandwidth
- Faster responses for large datasets
- Useful when the client only needs identifiers

### Disadvantages
- Client must make additional requests to fetch details → increases total network round-trips → increases latency

## Implications of returning full objects

### Advantages:
- Client receives all required data in one response → fewer API calls → simpler client logic.
 
### Disadvantages:
- Larger JSON payloads.
- More memory and processing required on the client.
- Slower for mobile or low-bandwidth environments.
 
## Conclusion:
Returning full objects improves convenience and reduces client complexity, while returning only IDs improves performance and reduces bandwidth usage.
The best choice depends on the expected client workload and performance constraints.


# Idempotency of DELETE in My Implementation

In my implementation, the `DELETE /rooms/{roomId}` operation is idempotent.

## Behavior on the First Successful DELETE:
- If the room exists and has no sensors, it is removed from the in-memory map.
- The server returns `204 No Content`.

## Behavior on Subsequent Identical DELETE Requests for the Same `roomId`:
- The room no longer exists in the map.
- The server returns `404 Not Found`, but the server state does not change any further.

## Key Property of Idempotency:
Performing the same operation multiple times has the same effect on server state as performing it once.

Even though the HTTP status code differs (`204` on first delete, `404` afterwards),
the resulting state of the system is the same: **the room is absent**.

Therefore, **the DELETE operation is idempotent** in this design.

---

# Part 3: What Happens If the Client Sends the Wrong Content-Type?

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that the POST method only accepts JSON input.

If a client sends data in another format (e.g., `text/plain`, `application/xml`, or no content type at all),
jax-rs performs content negotiation and detects that it cannot find a suitable message body reader to convert
the incoming payload into a Sensor object.

## Technical Consequences:
- JAX-RS rejects the request before the method is even executed.
- The framework automatically returns:
  - **415 Unsupported Media Type**

## Why This Happens:
- JAX-RS uses built-in and registered providers (like Jackson) to convert JSON into Java objects.
- If the incoming Content-Type does not match any available provider:
  - No deserializer can handle the payload.
  - JAX-RS aborts the request.
  - The resource method is never called.

## Summary:
- `@Consumes(MediaType.APPLICATION_JSON)` ensures that:
  - The server only accepts JSON.
  - Invalid formats are rejected early.
  - The client receives a clear **415 error**.
  - The API remains predictable and safe.



# Why Query Parameters Are Better for Filtering Collections

Using `@QueryParam` for filtering (e.g., `/sensors?type=CO2`) is the preferred RESTful design for search and filtering operations.

## 1. Query parameters express optional filtering

Filtering is not a new resource — it is a variation of the same collection.

Query parameters allow:

- `/sensors` → all sensors
- `/sensors?type=CO2` → filtered sensors
- `/sensors?status=ACTIVE&type=CO2` → multiple filters

Path parameters cannot express optional filters cleanly.

## 2. Path parameters imply a hierarchical resource

A URL like:

`/sensors/type/CO2`

suggests that “type” is a sub‑resource, which is not true.

Type is just a filter, not a child resource.

This breaks REST semantics.

## 3. Query parameters allow multiple filters

to support:
```
/sensors?type=CO2&status=ACTIVE&minValue=10
```

the path‑based filtering becomes messy:
```
/sensors/type/CO2/status/ACTIVE/minValue/10
```
This approach is harder to read, harder to maintain, and not idiomatic REST.

## 4. Query parameters are standard for search

Almost all modern APIs use query parameters for filtering:
- GitHub API
- Google APIs
- Stripe API
-AWS APIs 
This makes your API predictable and familiar to developers.

## 5. Better caching and indexing 
CDNs and proxies can cache:
- `/sensors`
- `/sensors?type=CO2`
Path‑based filtering creates many unnecessary URL variations.

### Summary 
@QueryParam is superior because it:
- Represents optional filters correctly 
- Keeps URLs clean and REST‑compliant 
- Supports multiple filters easily 
- Matches industry standards 
- Improves caching and client usability



# Part 4

## Benefits of the Sub‑Resource Locator Pattern

The Sub‑Resource Locator pattern allows a parent resource (e.g., `/sensors`) to dynamically return another resource class (e.g., `SensorReadingResource`) to handle deeper nested paths such as:

- `/sensors/{id}/readings`
- `/sensors/{id}/readings/{readingId}`

### 1. Separation of concerns

Instead of placing all logic inside one massive controller, each resource class focuses on a single domain:

- `SensorResource` → sensor metadata
- `SensorReadingResource` → historical readings

This keeps classes small, readable, and maintainable.

### 2. Cleaner, more scalable architecture

As APIs grow, nested paths become complex:

- `/sensors/{id}/readings`
- `/sensors/{id}/readings/{rid}`
- `/sensors/{id}/readings/latest`
- `/sensors/{id}/readings/stats`

Without sub‑resource locators, all of these would be forced into one giant class.

With locators, each nested domain gets its own class, making the API easier to extend.

### 3. Dynamic binding based on runtime values

The locator receives the `{sensorId}` and constructs a resource instance bound to that specific sensor.

This allows:
- Per‑sensor state
- Cleaner validation
- More intuitive code structure

### 4. Better organisation for large teams

different developers can work on:
- Sensor logic 
- Reading logic 
- Analytics logic 
…without touching the same file or causing merge conflicts.

### 5. More REST‑aligned design

the REST approach encourages modeling resources as a hierarchy.
Sub‑resource locators map this hierarchy directly into code:

e.g., `Sensor` → `SensorReadings` → `IndividualReading`
This mirrors the URL structure and improves conceptual clarity.

## Summary
The Sub‑Resource Locator pattern reduces complexity, improves maintainability, and keeps large APIs modular.
It prevents “God classes” and allows each nested resource to evolve independently.



# Part 5

## Why HTTP 422 Is More Accurate Than 404 for Missing Linked Resources

When a client submits a JSON payload that contains a reference to another resource (e.g., "roomId": "XYZ"), the request itself is syntactically valid:

- The JSON is well‑formed
- The fields are correct
- The structure matches the API contract

The problem is semantic:

- The referenced resource (roomId) does not exist.

## Why 404 is not ideal

**404 Not Found** means:
> “The requested URL does not exist.”

But in this scenario:
- The URL does exist (`POST /sensors`)
- The payload contains the invalid reference
- The client is not requesting `/rooms/XYZ` directly

So 404 does not accurately describe the error.

## Why 422 Unprocessable Entity is more accurate

**HTTP 422** means:
> “The server understands the request, but semantic validation failed.”

This matches the situation perfectly:
- The JSON is valid
- The structure is valid
- The server can parse it
- But the meaning of the data is invalid (because the referenced room does not exist)

Thus, 422 communicates:
> “Your request is well‑formed, but the data you supplied cannot be processed.”

## Summary
| Status Code | Meaning | Correctness |
|--------------|---------|--------------|
| **404 Not Found** | URL does not exist | Incorrect — the URL exists |
| **400 Bad Request** | Invalid syntax | Incorrect — JSON is valid |
| **422 Unprocessable Entity** | Semantic validation failed | Perfect match |
 
Therefore, 422 is the most semantically accurate status code for missing linked resources inside a valid JSON payload.



**Cybersecurity Risks of Exposing Java Stack Traces**

From a cybersecurity standpoint, exposing internal Java stack traces to external API consumers is extremely dangerous.

A stack trace reveals internal implementation details that attackers can exploit.



1\. Technology Fingerprinting

A stack trace exposes:

The exact Java version

The JAX‑RS implementation (Jersey, RESTEasy, etc.)

Internal libraries and versions

Frameworks and server containers

Attackers use this information to identify known vulnerabilities in those components.



2\. File Paths and Server Structure

Stack traces often reveal:

Absolute file paths

Directory layouts

Package names

Class names and method names

This helps attackers map your server’s internal structure, making targeted attacks easier.



3\. Sensitive Logic Exposure

Stack traces can reveal:

Internal business logic

Validation rules

Hidden endpoints

Database interaction layers

Names of private classes and methods

This information can be used to craft precise malicious requests.



4\. Injection Attack Assistance

If an exception originates from:

SQL queries

JSON parsing

XML parsing

File handling



…the stack trace may expose:

SQL statements

Table names

Column names

ORM mappings

This dramatically increases the success rate of SQL injection or deserialization attacks.



5\. Denial‑of‑Service (DoS) Amplification

Verbose stack traces:

Increase response size

Increase CPU load

Make it easier to craft requests that trigger expensive error paths

Attackers can exploit this to degrade or crash the service.



Summary

Exposing stack traces gives attackers:

What technology you use

How your code is structured

Where your files live

What your internal logic looks like

What libraries and versions you run

Potential injection points

This is why professional APIs never expose stack traces and always use a global 500 mapper.



**Why JAX‑RS Filters Are Better for Logging Than Manual Logger Calls**

Logging is a cross‑cutting concern: it applies to every endpoint, regardless of business logic.

JAX‑RS filters provide a clean, centralised way to implement this.



1\. Centralised Logging (Single Responsibility Principle)

If logging is implemented inside each resource method:

Every method must contain Logger.info() calls

Logging logic becomes duplicated

Resource classes become cluttered

Changes require editing many files

Filters solve this by placing all logging in one class, keeping resource classes focused on business logic.



2\. Guaranteed Coverage Across the Entire API

A filter intercepts:

Every request

Every response

Even requests that never reach a resource method

Even errors handled by exception mappers

Manual logging inside resource methods cannot guarantee this coverage.



3\. Consistent Logging Format

Filters ensure:

Every request is logged the same way

Every response is logged the same way

No developer forgets to add logging

No inconsistent formatting

This is essential for debugging and monitoring.



4\. Better Maintainability and Scalability

If you later decide to:

Add correlation IDs

Log execution time

Log headers

Log payload sizes

…you only update one filter, not 20 resource classes.



5\. Cleaner, More Professional API Design

Resource classes should contain:

Business logic

Validation

Domain rules

They should not contain infrastructure concerns like logging.

Filters keep your architecture clean and aligned with enterprise best practices.



Summary

Using JAX‑RS filters for logging is superior because it:

Centralises logging

Ensures consistent behaviour

Reduces duplication

Improves maintainability

Guarantees full API coverage

Keeps resource classes clean and focused

This is why filters are the industry‑standard approach for cross‑cutting concerns.





