# Codificador Médico CDI – Versión Java (Spring Boot)

Backend del Codificador Médico CDI para el Servicio de Medicina Interna.
Implementado con Spring Boot 2.7 + Java 8, empaquetable como WAR para despliegue en Apache Tomcat.

## Descripción

Aplicación para la estandarización de **diagnósticos principales en Medicina Interna, basado en el registro RECALMIN 2023**.
Permite la búsqueda interactiva y la codificación CIE-10-ES y optimización APR-GDR:

- **Motor de búsqueda server-side** agnóstico al orden de palabras.
- **Árboles de decisión dinámicos** generados desde el texto normativo CSV.
- **API REST** (`/api/all`, `/api/search?q=...`) para hidratación del frontend.
- **Frontend embebido** en `src/main/resources/static/`.

## Requisitos

- Java 8+
- Maven 3.6+

## Ejecución local

Asegúrese de tener el archivo `base_datos_recalmin.csv` en la raíz del proyecto y ejecute:

```bash
mvn spring-boot:run
```

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
