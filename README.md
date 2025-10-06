# Aplicación de Pruebas de Rendimiento

Aplicación mínima de Spring Boot junto con un plan de pruebas de carga en
JMeter.

¿Qué incluye?

- Aplicación Spring Boot con dos endpoints:
    - POST /auth/login → devuelve `{"access_token": "..."}`
    - GET /api/products/{id} → requiere `Authorization: Bearer <access_token>`
- Plan de pruebas JMeter `jmeter/test_plan.jmx`:
    - Thread Group: 60 usuarios, Ramp-Up 60 s, Loop infinito con duración 300
      s (5 minutos)
    - Uniform Random Timer: 200–800 ms (pausa entre acciones)
    - Login (Sampler) con JSON Extractor para guardar `access_token`
    - Petición de negocio (Sampler) con cabecera Authorization: Bearer $
      {access_token}
    - CSV Data Set Config apuntando a `jmeter/data/products.csv` con nombre de
      variable `productId` y modo de compartición "All threads" (valores
      distintos por hilo)

## Construcción y ejecución

1. Compilar la aplicación:

```shell
   mvn -DskipTests package
```

2. Ejecutarla:

```shell
   mvn spring-boot:run
```

La aplicación escucha en http://localhost:8080.

## Endpoints

- Login:
    - POST http://localhost:8080/auth/login
    - Cuerpo (JSON): `{"username":"user","password":"pass"}`
    - Respuesta (JSON): `{"access_token":"<uuid>", "token_type":"bearer"}`

- Petición de negocio:
    - GET http://localhost:8080/api/products/{id}
    - Cabeceras: `Authorization: Bearer <access_token>`

## Ejecutar el plan de JMeter

- Abrir `jmeter/test_plan.jmx` en Apache JMeter (recomendado 5.6.x) y ejecutar.
- O ejecutar en modo no-GUI (ejemplo):

```shell
jmeter -n -t jmeter/test_plan.jmx -l jmeter/results.jtl -e -o jmeter/report
```

Asegúrate de que la aplicación Spring Boot esté ejecutándose antes de lanzar la
prueba.


## Ejecutar la prueba con K6

Requisitos: instalar K6 (https://k6.io/)

El script `k6/test.js` implementa un escenario diferente:
- Executor constant-arrival-rate con 300 req/s, `duration` 5m, `preAllocatedVUs` 60 y `maxVUs` 200
- Umbrales: p(95) < 200 ms y tasa de fallos < 0.5%
- Realiza login en `setup()` contra `/auth/login` y usa el `access_token` en cada petición `GET /api/products/{id}`
- Pausa aleatoria 200–800 ms entre solicitudes

Ejemplo de ejecución:

```bash
export BASE_URL=http://localhost:8080
# Opcional: credenciales
export USERNAME=user
export PASSWORD=pass

k6 run k6/test.js
```

### ¿Qué es constant-arrival-rate en K6?

- Es un ejecutor que programa el inicio de iteraciones a una tasa fija (por ejemplo, 300 solicitudes por segundo), independiente del número de usuarios virtuales (VUs) disponibles en ese instante.
- K6 intentará mantener esa tasa adaptando dinámicamente la cantidad de VUs activos entre `preAllocatedVUs` (reservados al inicio) y `maxVUs` (límite superior) para cumplir el ritmo objetivo.
- Cada iteración ejecuta la función `default` una vez. Si la lógica interna incluye esperas (por ejemplo, `sleep(0.2–0.8)` segundos para simular think time) o la API responde lento, K6 puede necesitar más VUs para seguir alcanzando la tasa objetivo.
- Si el sistema bajo prueba se degrada y ni siquiera aumentando VUs hasta `maxVUs` se alcanza la tasa configurada, K6 reportará que la tasa real fue inferior a la objetivo (útil para detectar saturación de capacidad).

### Similitudes con el plan de JMeter

- Duración: ambos ejercicios están pensados para 5 minutos de ejecución.
- Flujo de negocio: primero se hace `login` para obtener `access_token` y luego se invoca el endpoint de negocio `GET /api/products/{id}` con la cabecera `Authorization: Bearer …`.
- Think time: en ambos se introduce una pausa aleatoria de 200–800 ms entre solicitudes para simular comportamiento humano y evitar ráfagas sincronizadas.
- Variación de productos: se consultan distintos IDs de producto durante la prueba.

### Diferencias clave entre K6 y JMeter en este proyecto

- Modelo de carga:
  - K6 (constant-arrival-rate) fija una tasa de llegadas (RPS). El foco está en el throughput objetivo y K6 ajusta VUs para mantenerlo.
  - JMeter usa un Thread Group con 60 usuarios, ramp-up de 60 s y "loop infinito" con duración controlada a 5 min. El foco está en la concurrencia (usuarios) y la tasa resultante es consecuencia de tiempos de respuesta y pausas.
- Control del ritmo:
  - K6 garantiza el intento de mantener 300 req/s constantes.
  - JMeter no garantiza RPS constante; para algo similar se necesitarían componentes como Throughput Shaping Timer + Concurrency Thread Group.
- Gestión de datos:
  - JMeter usa `CSV Data Set Config` para asegurar productos distintos por hilo.
  - K6 selecciona IDs aleatorios dentro de un rango (p. ej., 1001–1100). Puede replicarse un dataset fijo si se desea (p. ej., cargando un CSV en `setup()` y asignando por VU).
- Métricas y umbrales:
  - K6 define umbrales (`thresholds`) como p95 < 200 ms y tasa de fallos < 0.5% que hacen fallar la ejecución si no se cumplen.
  - JMeter se apoya en listeners/reportes (p. ej., HTML report) y aserciones; por defecto no "falla" la ejecución según percentiles a menos que se configuren Assertions o el plugin de PerfMon/Backends.
- Arranque y escalado:
  - K6 parte con `preAllocatedVUs=60` y puede escalar hasta `maxVUs=200` automáticamente según necesidad para sostener la tasa.
  - JMeter fija 60 hilos y mantiene ese nivel de concurrencia tras el ramp-up.
