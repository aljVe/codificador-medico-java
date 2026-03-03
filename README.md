# Codificador Médico CDI – Versión Java (Spring Boot)

Backend del Codificador Médico CDI para la estandarización de **diagnósticos principales en Medicina Interna, basado en las directrices del registro RECALMIN 2023** y la optimización de los Grupos Relacionados por el Diagnóstico (APR-GDR).

Esta versión del proyecto está implementada en **Spring Boot 2.7 y Java 8**, diseñada para entornos corporativos o de servidores hospitalarios centralizados. Puede ejecutarse standalone de forma local o empaquetarse silenciosamente como un archivo `.war` para su despliegue en contenedores web tradicionales (ej. Apache Tomcat).

---

## 🌟 Características Principales

- **Arquitectura Cliente-Servidor:** Separa la capa lógica y la de presentación, permitiendo centralizar la base de datos normativa en un servidor seguro.
- **Motor de Búsqueda Server-Side:** La API REST en Java expone un endpoint de cálculo *agnóstico al orden de palabras*, delegando el peso computacional al backend.
- **Árboles de Decisión Dinámicos con Código Exacto:** Modela en memoria caché las opciones complejas de diagnóstico mediante estructuras orientadas a objetos (`TreeNode`, `TreeOption`), y resuelve ensamblando el código CIE-10 final y exacto basado en el árbol construido desde el CSV maestro.
- **Frontend Embebido:** Incluye la Single Page Application (SPA) lista para servirse estáticamente en la raíz del backend.
- **Cobertura de Tests:** Verifica automáticamente casos de prueba límite (ej. manejo de aislamientos por comas/corchetes) usando `JUnit`.

## 🚀 Inicio Rápido

### Requisitos Previos

- **Java Development Kit (JDK) 8** o superior.
- **Apache Maven 3.6** o superior.

### Ejecución Local

1. Verifique que el archivo maestro `base_datos_recalmin.csv` está en la raíz del proyecto.
2. Compile y ejecute la aplicación a través de Maven:

```bash
mvn spring-boot:run
```

### Testing

El proyecto contiene una suite de pruebas que evalúan los algoritmos de búsqueda y la lógica del parseo del CSV. Para correr las pruebas manuales:

```bash
mvn test
```

## 📐 Estructura del Código

El código base sigue una estructura clásica de MVC (Model-View-Controller) adaptada a un contexto REST:

```text
src/main/java/com/hospital/cdi/
├── BuscadorApplication.java      # Entrypoint / Config para Tomcat (SpringBootServletInitializer)
├── controller/
│   └── SearchController.java     # Controladores REST (/api/search, /api/all)
├── model/
│   ├── MedicalItem.java          # POJO Entidad principal de datos médicos
│   ├── TreeNode.java             # Entidad recursiva de pregunta/opción
│   └── TreeOption.java           # Hoja o rama del árbol de decisión
└── service/
    └── DataService.java          # Inyección de caché, parser del CSV y lógica FuzzySearch

src/main/resources/static/        # Frontend (SPA embebida en Java)
src/test/java/                    # Suite de pruebas unitarias y de integración
```

---

*Backend diseñado con foco en la compatibilidad extendida (Java 8) para la integración en la infraestructura tecnológica del sector salud.*
