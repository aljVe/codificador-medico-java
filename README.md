# Codificador Médico CDI – Versión Java (Spring Boot)

Backend del Codificador Médico CDI para el Servicio de Medicina Interna.
Implementado con Spring Boot 2.7 + Java 8, empaquetable como WAR para despliegue en Apache Tomcat.

## Descripción

Aplicación Java que sirve una REST API y un frontend estático para codificación CIE-10-ES:

- **Motor de búsqueda server-side** agnóstico al orden de palabras.
- **Árboles de decisión dinámicos** generados desde el texto normativo CSV.
- **API REST** (`/api/all`, `/api/search?q=...`) para hidratación del frontend.
- **Frontend embebido** en `src/main/resources/static/`.

## Requisitos

- Java 8+
- Maven 3.6+

## Ejecución local

```bash
mvn spring-boot:run
```

Acceda en: [http://localhost:8080](http://localhost:8080)

## Estructura del proyecto

```
src/main/java/com/hospital/cdi/
├── BuscadorApplication.java     # Entrypoint Spring Boot
├── controller/
│   └── SearchController.java     # REST API (/api/search, /api/all)
├── model/
│   ├── MedicalItem.java          # Entidad principal
│   ├── TreeNode.java             # Nodo de árbol de decisión
│   └── TreeOption.java           # Opción dentro de un nodo
└── service/
    └── DataService.java          # Carga CSV, parsing y búsqueda

src/main/resources/static/        # Frontend embebido
src/test/java/                    # Tests JUnit
```

## Tests

```bash
mvn test
```

## Autor

**Alejandro Venegas Robles** — MIR Medicina Interna
Hospital Clínico Universitario Lozano Blesa, Zaragoza
alejandro2196vr@gmail.com
