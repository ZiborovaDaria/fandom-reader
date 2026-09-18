## ADDED Requirements

### Requirement: Store romantic and platonic relationships separately
For portal imports, the system SHALL store relationship tags that use "/" as romantic pairings and SHALL store relationship tags that use "&" as platonic relationships in a separate platonic collection, and MUST NOT place "&" tags into the romantic pairings list.

#### Scenario: Slash becomes romantic pairing
- **WHEN** an AO3 or Ficbook import contains a Relationships value with "/"
- **THEN** the system MUST store it as a romantic pairing

#### Scenario: Ampersand becomes platonic relationship
- **WHEN** an AO3 or Ficbook import contains a Relationships value with "&" and without treating it as "/"
- **THEN** the system MUST store it as a platonic relationship and MUST NOT list it among romantic pairings

#### Scenario: Canonical keys do not collapse & into /
- **WHEN** platonic and romantic forms of related names are stored
- **THEN** the system MUST keep distinct identities so that "&" platonic keys are not rewritten into "/" romantic keys
