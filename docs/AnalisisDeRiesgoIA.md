# Análisis de Riesgos del Proyecto

---

## 1. Riesgos Técnicos

### 1.1 Uso de SQLite como base de datos central

**Descripción**
SQLite funciona bien para desarrollo local pero tiene limitaciones de concurrencia cuando varios usuarios escriben al mismo tiempo (docentes cargando notas, alumnos inscribiéndose, administrativos modificando datos).

**Probabilidad:** Media | **Impacto:** Alto

**Consecuencias**
- Bloqueos de escritura
- Lentitud en operaciones concurrentes
- Dificultad para escalar el sistema

---

### 1.2 Motor de correlatividades mal definido

**Descripción**
La validación automática de correlatividades puede volverse compleja si existen:
- Correlatividades múltiples
- Correlatividades recursivas
- Planes de estudio distintos

**Probabilidad:** Alta | **Impacto:** Alto

**Consecuencias**
- Inscripciones incorrectas
- Errores en reglas académicas
- Dificultad para mantener el algoritmo

---

### 1.3 Riesgo de integridad de datos

**Descripción**
El sistema depende fuertemente de:
- Correlatividades
- Evaluaciones
- Historial académico

Un error en las relaciones puede romper la trazabilidad académica.

**Probabilidad:** Media | **Impacto:** Alto

---

### 1.4 Uso de Spark Framework (micro-framework)

**Descripción**
Spark es muy liviano pero:
- Tiene menos soporte que Spring
- Menos herramientas integradas
- Menos documentación en comparación

**Probabilidad:** Media | **Impacto:** Medio

---

### 1.5 Seguridad de credenciales mal implementada

**Descripción**
El requerimiento menciona cifrado simétrico para contraseñas, lo cual es conceptualmente incorrecto (debería usarse hashing).

Esto puede llevar a:
- Implementación insegura
- Malas prácticas de seguridad

**Probabilidad:** Alta | **Impacto:** Alto

---

### 1.6 Falta de pruebas suficientes

**Descripción**
Aunque se menciona JUnit, no se especifica:
- Cobertura de testing
- Pruebas de integración
- Pruebas de reglas académicas

**Probabilidad:** Media | **Impacto:** Medio

---

## 2. Riesgos Organizacionales

### 2.1 Organización rotativa del equipo

**Descripción**
El equipo rota entre roles:
- Backend
- Frontend
- Tester

Esto puede generar:
- Pérdida de especialización
- Falta de continuidad en módulos

**Probabilidad:** Alta | **Impacto:** Medio

---

### 2.2 Falta de liderazgo técnico claro

**Descripción**
No se menciona:
- Arquitecto
- Líder técnico
- Responsable de decisiones técnicas

**Probabilidad:** Media | **Impacto:** Alto

---

### 2.3 Dependencia de conocimiento individual

**Descripción**
Si un miembro desarrolla módulos críticos (ej: correlatividades), el resto puede no comprender el funcionamiento.

**Probabilidad:** Media | **Impacto:** Medio

---

## 3. Riesgos de Planificación

### 3.1 Ausencia de plazo definido

**Descripción**
El documento indica: *"Plazo estimado: Ni idea"*

Esto implica ausencia de:
- Planificación
- Cronograma
- Entregables

**Probabilidad:** Alta | **Impacto:** Alto

---

### 3.2 Alcance funcional amplio

**Descripción**
El sistema incluye:
- Gestión académica completa
- Correlatividades
- Inscripciones automáticas
- Gestión de notas
- Reportes

Para un equipo de 5 estudiantes, puede ser demasiado alcance.

**Probabilidad:** Alta | **Impacto:** Medio

---

### 3.3 Falta de priorización de funcionalidades

**Descripción**
No se identifican:
- MVP
- Funcionalidades críticas
- Fases de desarrollo

**Probabilidad:** Media | **Impacto:** Medio

---

### 3.4 Riesgo de integración tardía

**Descripción**
Frontend, backend y base de datos pueden desarrollarse en paralelo sin integración temprana.

**Probabilidad:** Media | **Impacto:** Medio

---

## 4. Riesgos Humanos

### 4.1 Diferencias de nivel técnico entre integrantes

**Descripción**
En proyectos académicos suele existir disparidad en conocimientos:
- Java
- Docker
- Testing

**Probabilidad:** Alta | **Impacto:** Medio

---

### 4.2 Baja disponibilidad de tiempo

**Descripción**
Los integrantes son estudiantes con:
- Otras materias
- Parciales
- Trabajos

**Probabilidad:** Alta | **Impacto:** Medio

---

### 4.3 Falta de experiencia en diseño de sistemas

**Descripción**
El proyecto incluye aspectos complejos:
- Modelo académico
- Correlatividades
- Integridad de datos

**Probabilidad:** Media | **Impacto:** Medio

---

### 4.4 Desmotivación o pérdida de compromiso

**Descripción**
En equipos pequeños es común que uno o dos miembros asuman más trabajo.

**Probabilidad:** Media | **Impacto:** Medio