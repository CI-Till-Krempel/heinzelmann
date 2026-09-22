# Product Requirements Document (PRD): Verteiltes LLM- & Job-Execution-Cluster

**Dokumentenversion:** 1.0  
**Status:** Entwurf  
**Technologie-Stack:** Kotlin Multiplatform (KMP), Ktor, Docker, Ray, Ollama  
**Zielgruppe:** IT-Administration, Software-Engineering-Teams  

---

## 1. Einleitung & Zielsetzung

### 1.1 Problemstellung
Moderne Softwareentwicklung und KI-Anwendungen erfordern zunehmend den Einsatz leistungsfähiger Large Language Models (LLMs) – insbesondere spezialisierter Coding-Agenten (z. B. auf Basis von Llama 3, StarCoder 2 oder CodeLlama). Der zentrale Cloud-Betrieb dieser Modelle ist extrem kostenintensiv und birgt Datenschutzrisiken. Gleichzeitig existiert in Unternehmen eine beträchtliche Menge ungenutzter Rechenleistung auf Client-Laptops (Windows und macOS), die insbesondere außerhalb der Arbeitszeiten (über Nacht) brachliegt.

### 1.2 Zielsetzung
Ziel dieses Projekts ist die Entwicklung eines plattformübergreifenden, verteilten Clustersystems. Das System nutzt die bestehende Hardware-Infrastruktur (Windows- und macOS-Laptops), um rechenintensive Aufgaben und lokale LLM-Inferenz über Nacht auszuführen. Die Steuerung erfolgt dynamisch und ressourcenschonend, ohne den regulären Tagesbetrieb der Mitarbeiter zu beeinträchtigen.

---

## 2. Zielgruppe & Anwendungsfälle

### 2.1 Zielgruppen
* **IT-Administratoren:** Benötigen eine zentrale Steuerung, Überwachung und Policysteuerung für den Fuhrpark.
* **Entwickler & Data Scientists:** Benötigen automatisierte Inferenz- und Trainingskapazitäten für KI-gestützte Coding-Agenten und Batch-Verarbeitung.

### 2.2 Hauptanwendungsfälle (Use Cases)
1. **Nächtliche Batch-Verarbeitung:** Ausführung komplexer LLM-Tasks (Code-Analyse, Refactoring-Pipelines, Testgenerierung) über Nacht. Die Ausführungsdauer steht hierbei vor der Latenz einzelner Anfragen.
2. **Verteiltes Rechnen mit Ray & Ollama:** Aufteilung großer KI-Modelle und Workloads über ein Netzwerk aus bis zu 30 Client-Knoten.
3. **Energiemessung & Power Management:** Aufwachen/Verhindern des Ruhezustands für geplante Jobs sowie Verbrauchsmessung über intelligente Steckdosen (z. B. Shelly, TP-Link Tapo).

---

## 3. Systemarchitektur & Komponenten

Das System besteht aus drei Hauptkomponenten sowie einer zentralen Server-Steuerung:

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

### 3.1 Client Daemons (macOS & Windows)
* **Plattformen:** macOS (LaunchDaemon) & Windows (Windows Service).
* **Funktionen:**
  * Hintergrunddienst mit minimalem Ressourcen-Footprint im Tagesbetrieb.
  * Live-Überwachung von CPU, GPU, RAM, Akkuladestand und Systemtemperatur.
  * Deaktivierung des automatischen Ruhezustands (Power Assertion / Keep-Awake) während aktiver Nacht-Jobs.
  * Start und Bereitstellung von isolierten Job-Laufzeiten via Docker-Container (z. B. Ray-Worker, Ollama-Instanzen).
  * Periodischer Heartbeat und Status-Reporting an den Control Server.

### 3.2 Control Server
* **Technologie:** Ktor Backend (Kotlin Multiplatform).
* **Funktionen:**
  * Zentrale Registrierung und Verwaltung aller aktiven Cluster-Knoten.
  * Job-Scheduling & Batch-Orchestrierung für nächtliche Aufgaben.
  * Verteilung von Tasks unter Berücksichtigung der verfügbaren Hardwareressourcen (Clustergröße bis ca. 30 Knoten).
  * Schnittstelle zu smarten Steckdosen zur Erfassung des tatsächlichen Energieverbrauchs.

### 3.3 Administrationstool
* **Technologie:** Compose for Desktop (Kotlin Multiplatform).
* **Funktionen:**
  * Dashboard zur Anzeige aktiver/inaktiver Knoten, Auslastung und Systemgesundheit.
  * Erstellung, Auslösung und Überwachung von Job-Pipelines.
  * Konfiguration von Richtlinien (Policies), z. B. Arbeitszeiten, Schwellenwerte für Akkuladung und Temperatur.

---

## 4. Funktionale Anforderungen (Functional Requirements)

| ID | Komponente | Anforderung | Priorität |
|---|---|---|---|
| **FR-01** | Daemon | Hintergrunddienst für macOS und Windows mit automatischem Systemstart. | Hoch |
| **FR-02** | Control Server | Registrierung von Client-Knoten und kontinuierliche Heartbeat-Überwachung. | Hoch |
| **FR-03** | Job Execution | Ausführung von Jobs in isolierten Docker-Containern auf den Clients. | Hoch |
| **FR-04** | LLM Engine | Anbindung von Ollama und Ray zur parallelen Inferenz verteilter Modelle. | Hoch |
| **FR-05** | Power Mgmt | Verhindern des Standby-Modus während geplanter Nacht-Jobs. | Hoch |
| **FR-06** | Energy Measuring| Integration von Smart-Plugs (Shelly, Tapo) zur Verbrauchserfassung. | Mittel |
| **FR-07** | Admin Dashboard | Dashboard für Live-Monitoring von Auslastung, Knotenanzahl und Logs. | Hoch |
| **FR-08** | Policy Control | Definieren von Zeitfenstern (z. B. 22:00 – 06:00 Uhr) für Batch-Jobs. | Hoch |

---

## 5. Nicht-funktionale Anforderungen (Non-Functional Requirements)

* **Performance & Footprint:** Der Daemon darf im Leerlauf (Tagesbetrieb) maximal 1% CPU und 50 MB RAM verbrauchen.
* **Plattformübergreifende Code-Basis:** Maximaler Code-Shares zwischen macOS, Windows und Server durch Nutzung von Kotlin Multiplatform (KMP).
* **Sicherheit:** Verschlüsselte Kommunikation (mTLS) zwischen Daemons, Control Server und Admin-Tool.
* **Skalierbarkeit:** Das System muss für Testgruppen (5–10 Geräte) sowie produktive Cluster (30+ Geräte) stabil skalieren.
* **Isolierung:** Alle Workloads müssen strikt in Containern laufen, um das Host-Betriebssystem vor Seiteneffekten zu schützen.

---

## 6. Meilensteine & Testplan

1. **Phase 1: Prototyp & Testgruppe (5–10 Geräte)**
   * Implementierung des KMP-Daemons und Ktor Control Servers.
   * Testen der Docker-Laufzeit und Power-Management-Funktionen.
2. **Phase 2: Verteilte Ausführung & Inferenz**
   * Integration von Ray und Ollama zur parallelen Ausführung von Coding-Agenten.
   * Einbindung des Admin-Dashboards.
3. **Phase 3: Rollout & Produktivcluster (30 Geräte)**
   * Ausrollen im produktiven Netzwerk und Validierung der nächtlichen Batch-Verarbeitung.
