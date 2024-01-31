-- pour MYSQL
CREATE TABLE exemples(
    id INT PRIMARY KEY AUTO_INCREMENT,
    entier INT,
    decimale DECIMAL(10, 2),
    floate float,
    chaine VARCHAR(255),
    texte TEXT,
    datee DATE,
    datetimee DATETIME,
    bool BOOLEAN
);
-- INT
-- INT
-- DECIMAL
-- FLOAT
-- VARCHAR
-- TEXT
-- DATE
-- DATETIME
-- BIT
-- pour POSTGRES
CREATE TABLE exemples (
    id SERIAL PRIMARY KEY,
    entier INTEGER,
    double double precision,
    reel NUMERIC(10, 2),
    chaine VARCHAR(255),
    texte TEXT,
    booleen BOOLEAN,
    date_col DATE,
    timestamp_col TIMESTAMP,
    intervalle INTERVAL
);
-- serial  int
-- int4     int
-- float8       double
-- numeric      double
-- varchar      string
-- text     string
-- bool     boolean
-- date     date
-- timestamp datetime
-- interval  time
/*
 public <type> getId {
 return this.Id;
 }
 
 public void setId(<type> id){
 this.id = id;
 }
 
 
 
 */
/*
 .NET
 public int Id
 {
 get { return id; }
 set { id = value; }
 }
 
 */