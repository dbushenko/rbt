# Use cases 

## [UCS-01] User signs in {#UCS-01}
*User:* not authenticated user.

*Preconditions:*

- user not authenticated;
- Form [UI-01: Sign in form](#UI-01) is shown.

*Postconditions:*

- User is authenticated;
- Form [UI-02: Home page](#UI-02) is shown.

*Main flow:*

1. The user specifies his credentials and signs into the system using [UI-01/A01: Login](#UI-01).
2. The system checks user credentials.
3. The system authenticates the user.
4. The system shows the form [UI-02: Home page](#UI-02) and shows the message [Successfull authentication](#M-01).

*Alternative flows:*

2a. *If* credentials are wrong *then* the system stops the use case and shows the message [Wrong credentials](#M-02).

### References from use case
[M-02](#M-02), [M-01](#M-01), [UI-02](#UI-02), [UI-01](#UI-01).

## [UCS-02] User signs up {#UCS-02}
TBD.
The use case will be based on [UI-01: Sign in form](#UI-01) and should be done prior [UCS-01: User signs in](#UCS-01).

# User interfaces

## [UI-01] Sign in form {#UI-01}

| ID/Caption       | Type     | Description                            |
|------------------|----------|----------------------------------------|
| A01  | Action   | Authenticates user                     |
| Login            |          |                                        |
|                  |          |                                        |
| A02 | Action   | Cancels the use case                   |
| Cancel           |          |                                        |
|                  |          |                                        |
| Email            | Email    | User identity.                         |
|                  |          | *Default value:* last specified email. |
|                  |          | *Mandatory*.                           |
|                  |          |                                        |
| Password         | Password | User password.                         |
|                  |          | *Mandatory*.                           |

### References to form
[UCS-02](#UCS-02), [UCS-01](#UCS-01).


## [UI-02] Home page {#UI-02}

The home page of the application as it will be described further.

### References to form
[UCS-01](#UCS-01).

# Messages

## [M-01] Successfull authentication {#M-01}
En-us: User authenticated successfully

## [M-02] Wrong credentials {#M-02}
En-us: Wrong credentials