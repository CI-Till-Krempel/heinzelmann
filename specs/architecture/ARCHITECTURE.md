# Systemarchitektur: Verteiltes LLM- & Job-Execution-Cluster

Dieses Dokument beschreibt die Architektur für die plattformübergreifende Verwaltung und Ausführung von verteilten Aufgaben (z. B. Coding-Agenten-Modelle über Ray und Ollama) auf bestehender Hardware (Windows & macOS).

---

## 1. Systemübersicht

Das System setzt auf eine dreigeteilte Architektur, um Hintergrundprozesse auf den Clients effizient zu steuern, zentral zu koordinieren und für Administratoren übersichtlich zu verwalten.

```
+-------------------------------------------------------+
|                    Admin Tool                         |
|         (Kotlin Multiplatform Desktop GUI)            |
+---------------------------+---------------------------+
                            |
                            v
+-------------------------------------------------------+
|                 Control Server                        |
|        (Ktor / Kotlin Multiplatform Backend)          |
+---------------------------+---------------------------+
                            |
           +----------------+----------------+
           |                                 |
           v                                 v
+-----------------------+         +-----------------------+
|  macOS Client Daemon  |         | Windows Client Daemon |
|  (Background Service) |         |  (Background Service) |
+-----------+-----------+         +-----------+-----------+
            |                                 |
            v                                 v
+-----------------------+         +-----------------------+
| Docker / Ollama / Ray |         | Docker / Ollama / Ray |
+-----------------------+         +-----------------------+
```

---

## 2. Technologie-Stack

* **Core Framework:** Kotlin Multiplatform (KMP)
  * **Shared Logic:** Netzwerkkommunikation, Datenmodelle, Job-Scheduling-Logik.
  * **Desktop GUI (Admin):** Compose for Desktop.
  * **Control Server:** Ktor Framework.
  * **Daemons:** Native Hintergrunddienste (macOS LaunchDaemon / Windows Service).
* **Containerisierung:** Docker / Containerd auf den Clients.
* **Verteiltes Rechnen & LLM Orchestration:** Ray Framework & Ollama.
* **Smart Home / Energy Management:** API-Integration für intelligente Steckdosen (z. B. Shelly, TP-Link Tapo) zur Verbrauchs- und Leistungsmessung.

---

## 3. Kernkomponenten

### 3.1 Client Daemons (macOS & Windows)
* **Funktion:** Leichtgewichtiger Hintergrunddienst, der auf den Laptops läuft.
* **Aufgaben:**
  * Überwachung von Systemauslastung (CPU, GPU, RAM) und Batteriestatus.
  * Verhinderung des Ruhezustands bei anstehenden/laufenden Nacht-Jobs.
  * Ausführung von Zuweisungen als Docker-Container (z. B. Ray-Knoten oder Ollama-Instanzen).
  * Status-Heartbeat an den Control Server.

### 3.2 Control Server
* **Funktion:** Zentrale Steuerungsinstanz des Clusters.
* **Aufgaben:**
  * Verwaltet die Liste aller aktiven Knoten (Clients).
  * Verteilt eingehende Jobs auf verfügbare Ressourcen.
  * Steuert nächtliche Batch-Jobs (z. B. rechenintensive Coding-Agenten), bei denen geringe Latenz sekundär ist.
  * Aggregiert Energie- und Leistungsinformationen.

### 3.3 Administrationstool
* **Funktion:** Grafische Oberfläche für IT-Administratoren.
* **Aufgaben:**
  * Live-Übersicht über alle aktiven/inaktiven Knoten und deren Auslastung.
  * Erstellung, Planung und Überwachung von Job-Pipelines.
  * Verwaltung von System-Policies und Energieprofilen.

---

## 4. Betriebsmodell & Job-Ausführung

1. **Tagbetrieb:** Clients verhalten sich ressourcenschonend im Hintergrund und priorisieren die Nutzerinteraktion.
2. **Nachtbetrieb (Batch-Processing):** 
   * Der Control Server aktiviert geplante Jobs (z. B. Ausführen von LLM-Inferenzen oder Trainingsläufen via Ray).
   * Die Laptops führen die Tasks in Docker-Containern aus.
   * Da die Ausführung über Nacht erfolgt, steht der Durchsatz vor der Ausführungsgeschwindigkeit einzelner Anfragen.
