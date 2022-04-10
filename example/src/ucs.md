# Use cases 

## [UCS-01] User signs in
*User:* not authenticated user.

*Preconditions:*

- user not authenticated;
- Form #ref:UI-01 is shown.

*Postconditions:*

- User is authenticated;
- Form #ref:UI-02 is shown.

*Main flow:*

1. The user specifies his credentials and signs into the system using #ref:UI-01/A01.
2. The system checks user credentials.
3. The system authenticates the user.
4. The system shows the form #ref:UI-02 and shows the message #refText:M-01.

*Alternative flows:*

2a. *If* credentials are wrong *then* the system stops the use case and shows the message #refText:M-02.

### References from use case
#refsFrom:UCS-01.

