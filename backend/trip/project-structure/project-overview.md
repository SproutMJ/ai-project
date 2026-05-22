백엔드 프로젝트 개요

### directory structure:
- root: /trip
- production code: /trip/src/main/java/org/mj/trip
- test code: /trip/src/test/java/org/mj/trip
- gradlew file: /trip/gradlew
- build.gradle file: /trip/build.gradle

### 기술 스택 (Tech Stack)
- language: java
- framework: spring boot
- database: h2
- orm/query builder: spring data jpa

### erd
The following defines the database schema:

Relationship:
- MEMBER 1:N TRIP_PLAN (A member owns multiple trip plans)

- Tables (DDL):

- MEMBER {
  member_id: bigint (PK)
  email: varchar (unique, nullable)
  nickname: varchar
  profile_image_url: varchar
  status: varchar (ACTIVE, WITHDRAWN)
  created_at: datetime
  updated_at: datetime
  deleted_at: datetime
}

- TRIP_PLAN {
  trip_plan_id: bigint (PK)
  member_id: bigint (FK -> MEMBER.member_id)
  title: varchar
  start_date: date
  end_date: date
  budget_amount: decimal
  region: varchar
  companion_count: int
  trip_purpose: varchar
  transport_mode: varchar
  meal_preference: varchar
  pace_level: varchar
  priority_types: varchar
  status: varchar (DRAFT, ACTIVE, ARCHIVED, DELETED)
  summary_text: text
  plan_data: json
  created_at: datetime
  updated_at: datetime
  deleted_at: datetime
}

### package structure
main folder: trip/src/main/java/org/mj/trip
- TripApplication.java (@SpringBootApplication)
- trip (domain)
  - controller
  - service
  - repository
  - domain
  - dto
- member (domain)
  - controller
  - service
  - repository
  - domain
  - dto
- auth (domain)
  - controller
  - service
  - repository
  - domain
  - dto
- common (domain)
  - config
  - exception
  - util

### coding style
- Controller Rules
  - Use @RestController.
  - Handle only URL mappings.
  - Do not include business logic.
  - Call only Service layer.
  - Return responses using DTOs whenever possible.
- Service Rules
  - Use @Service.
  - Contain core business logic.
  - Act as an intermediate layer between Controller and Repository.
  - Place transactions in the Service layer if needed.
- Repository Rules
  - Use @Repository or Spring Data JPA interfaces.
  - Handle only database access.
  - Do not include business logic.
- Domain Rules
  - Contain core models such as Entities and Value Objects.
  - Define data structure and domain rules.
- DTO Rules
  - Use for request and response objects only.
  - Do not expose Entities directly to external layers.
- Extra Rules
  - Never place feature classes in the root package
  - TripApplication.java only stays in the root package
  - Keep controllers thin
  - Keep business logic in services
  - Use DTOs for request/response
