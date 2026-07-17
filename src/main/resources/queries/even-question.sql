-- Question 2 (registration number ending in an even value)
-- For each employee, the count of employees younger than them within the same
-- department.
SELECT e.EMP_ID,
       e.FIRST_NAME,
       e.LAST_NAME,
       d.DEPARTMENT_NAME,
       COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT
FROM EMPLOYEE e
JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID
LEFT JOIN EMPLOYEE e2 ON e.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e.DOB
GROUP BY e.EMP_ID, e.FIRST_NAME, e.LAST_NAME, d.DEPARTMENT_NAME
ORDER BY e.EMP_ID DESC;
