create table employee (
  employee_id serial not null,
  first_name character varying(50) not null,
  last_name character varying(50) not null,
  email character varying(100) null,
  phone character varying(15) null,
  hire_date date not null,
  job_title character varying(50) null,
  department character varying(50) null,
  salary numeric(10, 2) null,
  manager_id integer null,
  city character varying(50) null default 'Hyderabad'::character varying,
  is_active boolean null default true,
  constraint employee_pkey primary key (employee_id),
  constraint employee_email_key unique (email),
  constraint employee_salary_check check ((salary > (0)::numeric))
);