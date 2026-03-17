# Gestor académico
La institución educativa atraviesa problemas comunes ya que  los procesos administrativos y académicos se sostienen en planillas y sistemas antiguos que no se comunican entre sí.
La falta de integración genera demoras, inconsistencias y una dependencia muy fuerte de tareas manuales. Además, la comunicación entre docentes, estudiantes y personal administrativo se vuelve un desafío permanente, lo que muestra que no solo se trata de registrar información, sino también de garantizar accesibilidad y transparencia.
El nuevo sistema que se plantea tiene que centralizar toda la información en una plataforma única y confiable, de manera que cada actor pueda acceder a los datos que necesita según su rol. Uno de los puntos más importantes que surge del relato es el registro académico de los estudiantes: no solo almacenar datos personales y de contacto, sino también poder seguir de manera clara qué materias cursaron, qué aprobaron, en qué etapa de la carrera se encuentran y qué opciones de inscripción tienen disponibles. En este sentido, aparece como un problema crítico la validación de correlatividades, ya que actualmente los estudiantes muchas veces intentan anotarse en materias para las que no cumplen los requisitos, y no existe una forma automática de detectar esas situaciones. La institución no solo quiere guardar constancia de qué materias se cursaron, sino también de la calificación final obtenida en cada una. Esto contribuye a la trazabilidad del estudiante y se convierte en un insumo indispensable para el seguimiento de su rendimiento académico. Incluso se piensa en una herramienta de análisis más avanzada, capaz de detectar patrones de riesgo de abandono o de destacar a quienes tienen un desempeño sobresaliente.
En cuanto a los actores principales del sistema, se distinguen claramente tres. En primer lugar, la Oficina de Alumnos, que asumiría el rol de administrador. Este sector necesita acceso completo para dar de alta y gestionar la información de estudiantes, carreras, materias, docentes y correlatividades, además de supervisar los registros académicos. En segundo lugar están los estudiantes, que requieren una interfaz de consulta que les permita revisar su avance en la carrera, ver qué materias tienen disponibles para cursar según correlatividades y acceder a sus calificaciones. Por último, aparecen los docentes, que no solo deben poder consultar listados de alumnos, sino también registrar las notas y verificar en qué materias y con qué rol han sido designados.

## Diagrama de diseño

> [!IMPORTANT] Importante
> El siguiente grafico de mermaid no soporta la clase asociación como tal, por ende lo único que pude hacer es hacer una Reificación. Es decir crear directamente la entidad en el modelo. (Esto es solo un comentario.)


```mermaid
classDiagram

%% Clases principales

class Persona {

-Dni

-Nombre

-Apellido

-Telefono

-Email

}

  

class Estudiante {

-Tipo Alumnno

}

  

class Profesor {

-Nro. Legajo

-Periodo

}

  

class Carrera {

-Nombre

-Duracion

}

  

class Plan_de_estudio {

-Carrera

-Año

}

  

class Materia {

-Codigo

-Nombre

}

  

%% Resolución de Clases de Asociación

class Evaluacion {

-Fecha

-Nota

}

  

class Condicion {

-Tipo Condicion

}

  

%% Enumeraciones

class Tipo_Alumnno {

<<enumeration>>

-Ingresante

-Avanzado

}

  

class Tipo_condicion {

<<enumeration>>

-Regular

-Aprobado

}

  

%% Herencia

Persona <|-- Estudiante

Persona <|-- Profesor

  

%% Relaciones estándar

Carrera "1" -- "0..*" Plan_de_estudio

Plan_de_estudio "1..*" -- "1..*" Materia

  

Profesor "0..*" -- "1..*" Materia : Ayudante

Profesor "1" -- "1..*" Materia : Responsable

  

%% Implementación de la clase de asociación: Evaluación

%% Reemplaza la relación N:M directa entre Estudiante y Materia

Estudiante "1" -- "0..*" Evaluacion : Realiza

Materia "1" -- "0..*" Evaluacion : Pertenece a

  

%% Implementación de la clase de asociación recursiva: Condición

%% Descompone la relación N:M recursiva de Materia consigo misma

Materia "1" -- "0..*" Condicion : Es requerida por

Materia "1" -- "0..*" Condicion : Depende de

  

%% Notas

note for Materia "Una materia no puede\nser correlativa de ella\nmisma"
```

## Historias de usuario

| **ID de HU**                                         | HU-EST-001                                                                                                                                                                                                                                                                                                                                       |
| :--------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Titulo**                                           | Gestión de Estudiantes                                                                                                                                                                                                                                                                                                                           |
| **Declaración**                                      | Como personal de la Oficina de Alumnos, quiero registrar y administrar la información de los estudiantes, para poder llevar un control claro de sus datos personales y su avance académico.                                                                                                                                                      |
| **Descripción Detallada**                            | El sistema debe permitir:<br><br>-  Registrar estudiantes ingresantes y avanzados.<br>- Guardar datos personales (DNI, nombre, apellido, dirección, contacto).<br>- Consultar y actualizar información en cualquier momento.<br>- Mantener un historial académico del estudiante (materias cursadas, correlatividades aprobadas, notas finales). |
| **Criterios de Validación(Criterios de aceptación)** | No se puede registrar un estudiante con un DNI duplicado.<br>Al actualizar información de un estudiante, los cambios deben persistir y ser visibles inmediatamente.<br>El sistema debe diferenciar entre estudiantes ingresantes y avanzados.<br>Se debe poder consultar un historial académico completo por estudiante.                         |
| **Tareas Asociadas a la Implementación**             | Crear modelo de datos para “Estudiante”.<br>Implementar validación de DNI único en la base de datos.<br>Desarrollar interfaz de alta, modificación y consulta de estudiantes.<br>Implementar consulta de historial académico por estudiante.                                                                                                     |

| **ID de HU**                                          | HU-MAT-001                                                                                                                                                                                                                                                                                    |
| :---------------------------------------------------- | :-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Titulo**                                            | Gestión de Materias                                                                                                                                                                                                                                                                           |
| **Declaración**                                       | Como administrador del sistema, quiero gestionar la información de las materias, para poder organizar la oferta académica y mantener actualizados los planes de estudio.                                                                                                                      |
| **Descripción Detallada**                             | El sistema debe permitir:<br>- Cargar y modificar materias de cada carrera.<br>- Registrar código, nombre y plan de estudios al que pertenece cada materia.<br>- Definir correlatividades entre materias.                                                                                     |
| **Criterios de Validación (Criterios de aceptación)** | No se puede registrar una materia con código duplicado.<br>Al consultar una materia, se debe mostrar su plan de estudios y correlatividades.<br>El sistema debe impedir inscripciones si no se cumplen las correlatividades.<br>Se debe poder actualizar correlatividades de manera dinámica. |
| **Tareas Asociadas a la Implementación**              | Crear modelo de datos para “Materia” y “Correlatividad”.<br>Implementar validación de código único para materias.<br>Desarrollar interfaz para alta, modificación y consulta de materias.<br>Programar lógica de validación de correlatividades en inscripciones.                             |

| **ID de HU**                                         | HU-CAR-001                                                                                                                                                                                                                                                                                             |
| :--------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Titulo**                                           | Gestión de Carreras                                                                                                                                                                                                                                                                                    |
| **Declaración**                                      | Como administrador del sistema, quiero gestionar la información de las carreras, para poder organizar los planes de estudio y relacionarlos con sus materias correspondientes.                                                                                                                         |
| **Descripción Detallada**                            | El sistema debe permitir:<br>- Cargar y modificar carreras ofrecidas por la institución.<br>- Registrar nombre, código y plan de estudio de cada carrera.<br>- Asociar cada carrera con su listado de materias.<br>- Consultar carreras disponibles y sus planes vigentes.                             |
| **Criterios de Validación(Criterios de aceptación)** | No se puede registrar una carrera con código duplicado.<br>Al consultar una carrera, se debe mostrar su listado de materias asociadas.<br>Los cambios en el plan de estudios deben reflejarse en la oferta académica.<br>El sistema debe permitir inactivar carreras antiguas sin perder su historial. |
| **Tareas Asociadas a la Implementación**             | Crear modelo de datos para “Carrera” y su relación con “Materia”.<br>Implementar validación de código único de carrera.<br>Desarrollar interfaz de alta, modificación y consulta de carreras.<br>Implementar lógica de asociación entre carrera <--> materias.                                         |

## Requerimientos funcionales
- Gestión de Estudiantes 
- Gestión de Profesores 
- Gestión de Materias 
- Gestión de Carreras 
- Gestión de los Planes de Estudio 
- Gestión Progreso de los Estudiantes
## Requerimientos no funcionales
- El sistema debe centralizar la información en una única plataforma y reemplazar las planillas y sistemas previos no integrados.
- El sistema debe contar con una interfaz sencilla que facilite la comunicación y consulta de información para usuarios (administrativos, docentes y estudiantes).