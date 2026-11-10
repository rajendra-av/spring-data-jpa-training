-- 1. department first (manager FK added later, because employee does not exist yet)
CREATE TABLE IF NOT EXISTS department (
    department_id   SERIAL       NOT NULL,
    department_name VARCHAR(50)  NOT NULL,
    location        VARCHAR(50)  DEFAULT 'Hyderabad',
    manager_id      INTEGER,
    budget          NUMERIC(12, 2),
    is_active       BOOLEAN      DEFAULT TRUE,
    CONSTRAINT department_pkey PRIMARY KEY (department_id),
    CONSTRAINT department_department_name_key UNIQUE (department_name),
    CONSTRAINT department_budget_check CHECK (budget >= 0)
);

-- 2. employee (department exists now; the FK targets the unique department_name column)
CREATE TABLE IF NOT EXISTS employee (
    employee_id SERIAL       NOT NULL,
    first_name  VARCHAR(50)  NOT NULL,
    last_name   VARCHAR(50)  NOT NULL,
    email       VARCHAR(100),
    phone       VARCHAR(15),
    hire_date   DATE         NOT NULL,
    job_title   VARCHAR(50),
    department  VARCHAR(50),
    salary      NUMERIC(10, 2),
    manager_id  INTEGER,
    city        VARCHAR(50)  DEFAULT 'Hyderabad',
    is_active   BOOLEAN      DEFAULT TRUE,
    CONSTRAINT employee_pkey PRIMARY KEY (employee_id),
    CONSTRAINT employee_email_key UNIQUE (email),
    CONSTRAINT fk_employee_department FOREIGN KEY (department)
        REFERENCES department (department_name) ON UPDATE CASCADE,
    CONSTRAINT employee_salary_check CHECK (salary > 0)
);

-- 3. close the loop: department.manager_id -> employee
ALTER TABLE department
    ADD CONSTRAINT IF NOT EXISTS fk_department_manager
    FOREIGN KEY (manager_id) REFERENCES employee (employee_id);

-- 4. project (department exists)
CREATE TABLE IF NOT EXISTS project (
    project_id    SERIAL        NOT NULL,
    project_name  VARCHAR(100)  NOT NULL,
    department_id INTEGER       NOT NULL,
    budget        NUMERIC(12, 2),
    start_date    DATE          NOT NULL,
    end_date      DATE,
    status        VARCHAR(20)   DEFAULT 'Active',
    CONSTRAINT project_pkey PRIMARY KEY (project_id),
    CONSTRAINT project_project_name_key UNIQUE (project_name),
    CONSTRAINT project_department_id_fkey FOREIGN KEY (department_id)
        REFERENCES department (department_id),
    CONSTRAINT project_budget_check CHECK (budget >= 0)
);

CREATE TABLE IF NOT EXISTS employee_project (
    employee_id    INTEGER      NOT NULL,
    project_id     INTEGER      NOT NULL,
    role           VARCHAR(50)  NOT NULL,
    hours_per_week NUMERIC(4, 1),
    CONSTRAINT employee_project_pkey PRIMARY KEY (employee_id, project_id),
    CONSTRAINT employee_project_employee_id_fkey FOREIGN KEY (employee_id)
        REFERENCES employee (employee_id),
    CONSTRAINT employee_project_project_id_fkey FOREIGN KEY (project_id)
        REFERENCES project (project_id)
);