-- Small fixture with the edge cases the two questions hinge on.
INSERT INTO DEPARTMENT VALUES (1, 'HR'), (2, 'Finance'), (3, 'Engineering');

-- Employees 3 and 6 share a date of birth: neither is younger than the other.
-- Employee 2 is born on 1 January so their age is the same under every
-- engine's year-difference rule.
INSERT INTO EMPLOYEE VALUES
    (1, 'John',    'Williams', DATE '1980-05-15', 'Male',   3),
    (2, 'Sarah',   'Johnson',  DATE '1990-01-01', 'Female', 2),
    (3, 'Michael', 'Smith',    DATE '1985-08-10', 'Male',   3),
    (4, 'Emily',   'Brown',    DATE '1992-11-30', 'Female', 1),
    (5, 'David',   'Jones',    DATE '1988-03-22', 'Male',   3),
    (6, 'Olivia',  'Davis',    DATE '1985-08-10', 'Female', 3);

-- The largest payment falls on the 1st of the month and must be ignored.
INSERT INTO PAYMENTS VALUES
    (1, 4, 99999.00, TIMESTAMP '2025-03-01 10:00:00'),
    (2, 2, 88000.00, TIMESTAMP '2025-03-15 09:30:00'),
    (3, 1, 75000.00, TIMESTAMP '2025-01-02 12:00:00'),
    (4, 3, 60000.00, TIMESTAMP '2025-02-28 18:45:00'),
    (5, 5, 91000.00, TIMESTAMP '2025-04-01 00:00:01');
