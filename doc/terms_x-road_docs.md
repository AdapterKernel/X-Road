# Terms of X-Road

## Version history

 Date       | Version | Description                                                     | Author
 ---------- | ------- | --------------------------------------------------------------- | --------------------
 06.07.2015 | 0.1     | Initial draft                                                   |
 23.02.2017 | 0.2     | Converted to Github flavoured Markdown, added license text, adjusted tables and identification for better output in PDF. Added explanation of monitoring service | Toomas Mölder 

## Table of Contents

<!-- toc -->

- [License](#license)
- [1 X-Road and X-Road Instance](#1-x-road-and-x-road-instance)
- [2 Participants of X-Road](#2-participants-of-x-road)
- [3 Trust services](#3-trust-services)
- [4 Roles of X-Road member](#4-roles-of-x-road-member)
  * [4.1 In terms of dataservice](#41-in-terms-of-dataservice)
  * [4.2 In terms of management of security server](#42-in-terms-of-management-of-security-server)
- [5 X-Road interfacing steps](#5-x-road-interfacing-steps)
- [6 Elements of X-Road technology](#6-elements-of-x-road-technology)
  * [6.1 Technology in general](#61-technology-in-general)
  * [6.2 X-Road internal components](#62-x-road-internal-components)
  * [6.3 X-Road external components](#63-x-road-external-components)
  * [6.4 Elements of X-Road software](#64-elements-of-x-road-software)
    + [6.4.1 Service and message](#641-service-and-message)
    + [6.4.2 Subsystems and access rights](#642-subsystems-and-access-rights)
  * [6.5 X-Road protocols](#65-x-road-protocols)
  * [6.6 Logging and security](#66-logging-and-security)
  * [6.7 Identificators and codes](#67-identificators-and-codes)

<!-- tocstop -->

## License

This document is licensed under the Creative Commons Attribution-ShareAlike 3.0 Unported License. To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/

## 1 X-Road and X-Road Instance

**X-Road instance** (*X-tee keskkond*)– legal, organizational and technical environment, enabling universal internet-based secure data exchange between the members of X-Road and limited to the participants administered by one governing authority.

> **X-tee keskkond** (*X-Road instance*) – X-tee liikmete vahelist turvalist ja tõestusväärtust tagavat internetipõhist andmevahetust võimaldav tehniline infrastruktuur ja organisatsiooniline keskkond.

**United/federated X-Road** (*X-tee ühendus*) – legal, organizational and technical environment, enabling universal internet-based secure data exchange between the members of united/federated X-Road instances

> **X-tee …** (*United/federated X-Road*) – ühendatud X-tee keskkondade liikmete vahelist turvalist ja tõestusväärtust tagavat internetipõhist andmevahetust võimaldav tehniline infrastruktuur ja organisatsiooniline keskkond.

**Local X-Road instance** (*kohalik X-tee keskkond*)

**External X-Road instance** (*väline X-tee keskkond*)

## 2 Participants of X-Road

**X-Road governing authority** (*X-teed haldav asutus*) – authority, that sets the requirements for using X-road and establishing the procedure for using X-Road, managing and regulating participants of X-Road.

**X-Road Center** (*X-tee keskus*) - participant of X-Road administering components of the X-Road software centre.

**X-Road member / member** (*X-tee liige / liige*) – participant of X-Road entitled to exchange data/messages on X-Road.

> **X-tee liige** on isik, kellel on õigus kasutada X-teed andmevahetuseks.

**Local member** (*… liige*) – a member entitled to exchange data/messages on the united X-Road and managed by governing authority of the local X-Road instance.

**United / Federated member** (*ühendatud X-tee liige)* – a member entitled to exchange data/messages on their behalf on the united X-Road, but managed by governing authority of the external X-Road instance.

**End user of dataservice** (*andmeteenuse lõppkasutaja*) – information system, part of information system or physical persoon, who uses data service through the inforamtion system of X-Road member.

> **Andmeteenuse lõppkasutaja** on infosüsteem või selle osa või füüsiline isik, kes kasutab andmeteenust X-tee liikme infosüsteemi vahendusel.

**Approved trust service provider** (*tunnustatud usaldusteenuse osutaja*) – participant of X-Road, who meets the requirements established by X-Road governing authority and has passed the process of recognition of X-Road trust service provider.

## 3 Trust services

**Approved certification service provider** (*tunnustatud sertifitseerimisteenuse osutaja*) – Provider of a trust service approved on X-Road, who provides at least following trust services approved on X-Road: service of authentication certificate of security server, service of signature certificate of a member, and sertificate validation service (OCSP).

**Approved timestamp service provider** (*tunnustatud ajatempliteenuse osutaja*) – Provider of a trust service approved on X-Road, who provides the timestamp service.

**Authentication certificate of security server** (*turvaserveri autentimissertifikaat*) – qualified certificate of e-stamp issued by certification service provider approved on X-Road and bound to security server, certifying authenticity of security server and used for authentication of security servers upon establishment of connection between security servers. Upon establishment of connection, it is checked from global configuration, if the security server trying to establish connection has registered the used authentication certificate in X-Road governing authority (i.e. the used authentication certificate is bound to the ID of security server).

> **Turvaserveri autentimissertifikaat** (*authentication sertificate (of security server)*) – X-teel tunnustatud sertifitseerimisteenuse osutaja väljastatud ja turvaserveriga seotud sertifikaat, mis tõendab turvaserveri autentsust ja mida kasutatakse turvaserverite autentimiseks turvaserverite vahelise ühenduse loomisel. Ühenduse loomisel kontrollitakse globaalsest konfiguratsioonist, kas ühendust luua üritav turvaserver on kasutatava autentimissertifikaadi X-tee keskuses registeerinud (st kas kasutatav autentimissertifkaat on seotud turvaserveri identifikaatoriga).

**Signature certificate of a member** (*liikme e-templi sertifikaat*) – qualified certificate of e-stamp issued by certification service provider approved on X-Road and bound to a member, used for verification of the integrity of mediated messages and association of the member with the message.

> **Liikme allkirjasertifikaat** (*signature sertificate (of member)*) – X-teel tunnustatud sertiftiseerimisteenuse osutaja väljastatud ja liikmega seotud e-templi sertifikaat, mida kasutatakse vahendatavate sõnumite tervikluse tõendamiseks ning tõendamaks liikme seost sõnumiga.

**Validation service** (OCSP) – Validation service of the validity of certificate issued by certification service provider approved on X-Road.

> **Kehtivuskinnituse teenus** (*OCSP*) – X-teel tunnustatud sertifitseerimisteenuse osutaja väljastatud sertifikaadi kehtivuse kinnituse teenus.

**Timestamp** – means data in electronic form which binds other data in electronic form to a particular time establishing evidence that the latter data existed at that time (EU No 910/2014)

> **Ajatempel** - elektroonilised andmed, mis seovad muud elektroonilised andmed kindla ajahetkega ja tõendavad, et need andmed olid sel ajahetkel olemas on.

## 4 Roles of X-Road member

### 4.1 In terms of dataservice

**Dataservice provider** (*andmeteenuse osutaja*) – member of X-Road responsible for dataservice provision, incl. granting the service SLA, managing the agreements with dataservice clients, granting access rights etc. Technically, dataservice provider is a party of interaction sending the response.

> **Andmeteenuse osutaja** on X-tee liige, kes osutab teistele liikmetele andmeteenust. Tarkvaras on andmeteenuse osutaja on interaktsiooni osapool, kes edastab vastussõnumi.

**Dataservice client** (*andmeteenuse kasutaja*) – member of X-Road responsible for using the dataservice in accordance with dataservice usage agreements. Technically, dataservice client is a party of interaction sending the request.

> **Andmeteenuse kasutaja** on X-tee liige, kes kasutab andmeteenust. Tarkvaras, on andmeteenuse kasutaja interaktsiooni osapool, kes edastab päringusõnumi.

**Dataservice host** (*andmeteenuse vahendaja*) - A member enabling access to X-Road services through their information system (as the provider or user of the service) for natural or legal persons, who need not be members of X-Road.

> **Andmeteenuse vahendaja** on X-tee liige, kes võimaldab enda organisatsioonivälistele füüsilistele või juriidilistele isikutele juurdepääsu andmeteenusele oma infosüsteemi vahendusel.

### 4.2 In terms of management of security server

**Security server owner** (*turvaserveri omanik*) – a member responsible for security server and creation of a secure data exchange channel.

> **Turvaserveri omanik** (*security server owner*) – liige, kes vastutab turvaserveri ja turvalise andmevahetuskanali loomise eest.

**Security server client** (*turvaserveri klient*) – a member or subsystem of a member, whose relation with security server is registered in X-Road governing authority and who can use the security server on behalf of a member to exchange data on X-Road.

> **Turvaserveri klient** (*security server client*) – liige või liikme alamsüsteem, kelle seos turvaserveriga on registreeritud X-tee keskuses ning kes saab turvaserverit kasutada liikme nimel andmete vahetamiseks X-teel.

**Security server host** (*turvaserveri majutaja*) – Member who provides security server hosting services to third parties and other members.

## 5 X-Road interfacing steps

**Affiliation of membership** (*X-teega liitumine*) – a process ending with becoming a member of X-Road. Becoming a member requires conclusion of affiliation contract and registration of data of the member (name and ID of the member) in X-Road central server. Requirements for affiliation are established by X-Road governing authority with relevant regulation/affiliation conditions

> **X-teega liitumine** – protsess, mis lõpeb X-tee liikmeks saamisega. Protsess sisaldab liitumistingimustega nõustumist ja liikme andmete registreerimist keskserveris. Liitumistingimused kehtestab X-teed haldav asutus.

**Registration of security server** (*turvaserveri registreerimine*) – a process, where organizational and technical capacity of a member of X-Road is created to enable contacting the information system of the member of X-Road via X-Road. The result is a member of X-Road, with whom a secure data exchange channel of X-Road can be established. To ensure this, at least one security server shall be bound to the member in the central server.

**Registration of subsystem** (*alamsüsteemi registreerimine*) – a process for establishing organizational and technical capacity to distinguish organizational users or user groups on the level of a subsystem. Technically, subsystems shall be registered as security server clients.

**Dataservice interfacing** (*andmeteenuse lisamine*) – a process, where a member of X-Road creates organizational and technical capacity for offering or using dataservice. Interfacing includes development of the service by the member as well as its setup in security server, conclusion of service usage contracts and granting access rights. In order to use the service, service provider as well as service client shall undergo interfacing.

**Interaction** (*interaktsioon*) – activation procedure of dataservice (single use), bilateral information exchange through dataservice, i.e. request of dataservice by the service client by sending a request, to which the service provider will send a response.

## 6 Elements of X-Road technology

### 6.1 Technology in general

**Core technology** (*tuumtarkvara*) – Component of X-Road software, ensuring integrity and verification value of messages between members. Core technology includes central server, configuration proxy and security server.

**Service technology** (*liidestustehnoloogia*) – Component of X-Road software, simplifying or enabling the use of core technology.

### 6.2 X-Road internal components

**Security server** (*turvaserver*) – standard software solution for using secure data exchange channel of X-Road and ensuring confidentiality, authenticity and integrity of messages/data exchanged on X-Road.

> **Turvaserver** – standardiseeritud tarkvara, mis tagab X-tee sõnumite vahendamise kooskõlas X-tee baasprokotollistikuga, tagades andmete konfidentsiaalsuse, autentsuse ja tervikluse.

**Central components** (*kesksed komponendid*) are central server and configuration proxy.

> **Kesksed komponendid** on keskuse poolt hallatavad tarkvara komponendid, mis tagavad X-tee turvalise andmevahetuskanali loomiseks ja andmeteenuste kasutamiseks vajaliku info kättesaadavuse X-tee liikmete turvaserveritele.

**Central server** (*keskserver*)

**Configuration proxy** (*konfiguratsioonivahendaja*)

### 6.3 X-Road external components

**Adapterserver** (EE: *liidestuskomponent*; FI: *sovitinpalvelu*)

> **Liidestuskomponent** on tarkvara, mis võimaldab alamsüsteemil vahetada turvaserveriga sõnumeid vastavalt X-tee sõnumiprotokollile.

**Information system** – a system including technological as well as organizational information processing of a member of X-Road

**Subsystem** – minimum part of information system of a member of X-Road, to which access to X-Road content service can be enabled.

> **Alamsüsteem** on tehnoloogiliselt ja organisatoorselt piiritletud X-tee liikme infosüsteemi osa, mida on X-teel võimalik andmeteenuse osutajana või kasutajana kindlaks määrata.

### 6.4 Elements of X-Road software

#### 6.4.1 Service and message

**Dataservice** (*andmeteenus*) – web-service executed by a member of X-Road, in order to enable access to the resources of information system of X-Road dataservice provider. The predefined request-response, sent by the information system of a member to the information system of another member and receiving agreed data in response.

> **Andmeteenus** on X-teel realiseeritud veebiteenus, mille kaudu toimub internetipõhine andmevahetus.

**Central service** (*keskne teenus*) – dataservice, in case of which the name of service provider is defined by the governing authority. The reason for such alias-name may be the need to assure the service provision (when the service provider changes) without a need to change access rights.

**Metadata service** (*metaandmete teenus*) – services between members executed by X-Road governing authority, enabling members of X-Road to get an overview of X-Road (e.g. enabling to get an overview of completed services and access rights needed for the consumption of services). Generally, it shall meet the description of X-Road service.

> **Metaandmete teenus** (*metadata service*) – X-tee keskuse realiseeritud liikmetevahelised teenused, mis võimaldavad X-tee liikmetel ja X-tee kasutajatel saada ülevaate X-teest (nt võimaldavad saada teenuste tarbimiseks vajaliku ülevaate realiseeritud teenuste ja pääsuõiguste kohta).

**Monitoring service** (*seireteenus*) – The X-Road monitoring solution is conceptually split into two parts: environmental and operational monitoring. 

- **Environmental monitoring** (*keskkonnaseire*) is the monitoring of the X-Road environment: details of the security servers such as operating system, memory, disk space, CPU load, running processes and installed packages, etc.

- **Operational monitoring** (*kasutuse seire*) is the monitoring of operational statistics such as which services have been called, how many times, what is the average response time, etc.

**Management service** (*haldusteenus*) – services provided by the X-Road governing organization to manage security servers and security server clients. Management services are implemented as standard X-Road services following X-Road message protocol.

> **Haldusteenus** (*management service*) – X-tee keskuse poolt pakutav teenus, mida saab kasutada ühe X-tee keskkonna piires turvaserverite ja turvaserveri klientide haldamiseks. Haldusteenus tagab X-tee toimimise (nt liidestumiseks vajalikud teenused). Haldusteenused järgivad X-tee sõnumiprotokolli.

**Message** (*sõnum*) – Data set meeting profile description and service description required by X-Road governing authority. Messages are divided into requests and responses. Message consists of headers and payload. Payload is a SOAP body that contains service specific content.

> **Sõnum** on vormindatud andmete kogum, mida vahetatakse andmeteenuse osutaja ja kasutaja vahel.

#### 6.4.2 Subsystems and access rights

**Access right** (*pääsuõigus*) in X-Road technology enables specifying the rights of security server clients (subsystems) to use dataservices.

> **Pääsuõigus** on andmeteenuse kasutamise võimaldamine X-tee tarkvaras.

**Access right group** (*pääsuõiguste grupp*) – set of security server clients (subsystems), enabling to grant access rights to the entire group of subsystems and to delegate administration of access rights to the group administrator. Logical name can be assigned to an access right group.

**Global access right group** (*globaalne pääsuõiguste grupp*) – access right group administered in the central server by the central server administrator, usable in the entire X-Road federation.

**Local access right group** (*lokaalne pääsuõiguste grupp*) – access right group administered in security server by security server administrator, usable only in the specific security server within one security server client.

**Global configuration** (*globaalne konfiguratsioon*) – a technical solution, through which X-Road governing authority regulates participants of X-Road. Global configuration includes XML-files, which are downloaded periodically from the central server of X-Road governing authority by security servers. Global configuration includes also following data: addresses and public keys of approved timestamp services; addresses and public keys of trust anchors (top CAs, timestamp services); public keys of intermediate CAs; addresses and public keys of OCSP services (if not available through the *Authority Information Access* extension of certificates); data of members of X-Road and their subsystems; addresses of registered security servers of members of X-Road; data of registered authentication certificates of security servers; registered clients of security servers; data of global access right groups.

> **Globaalne konfiguratsioon** (*global configuration*) – tehniline lahendus, mille kaudu X-tee keskus reguleerib X-tee osalisi. Globaalne konfiguratsioon koosneb XML-failidest, mida turvaserverid perioodiliselt X-tee keskuse keskserverist alla laadivad.

### 6.5 X-Road protocols

**Message Transport Protocol** \[PR-MESSTRANSP\] (*X-tee sõnumitranspordi protokoll*, lühidalt *transpordiprotokoll*) – communications protocol that is used by service client's and service provider's security servers to exchange messages with each other.

**Protocol for Downloading Configuration** \[PR-GCONF\] (*X-tee konfiguratsiooni allalaadimise protokoll*, lühidalt *konfiguratsiooniprotokoll*) – protocol that is used to distribute configuration to security servers of an X-Road instance.

**Federation Protocol** \[PR-FGONF\], currently same as\[PR-GCONF\] (*X-teede ühendamise protokoll*) – protocol that is used to distribute configuration between two federated X-Road instances.

**Message protocol** \[PR-MESS\] (*X-tee sõnumiprotokoll*, lühidalt *sõnumiprotokoll*) – protocol that is used between information systems and security servers in the X-Road system.

> **X-tee sõnumiprotokoll** on X-tee baasprotokollistiku osa, mis tagab turvaserveri võime töödelda sõnumeid.

**Service Metadata Protocol** \[PR-META\] (*X-tee teenuste metaandmete protokoll*, lühidalt *metaandmete protokoll*) – protocol that describes methods that can be used by X-Road participants to discover what services are available to them and download the WSDL files describing these services.

### 6.6 Logging and security

Secure log?

**Message log** (*päringulogi*) – log, where changed messages are logged and provided with batch signature.

> **Päringulogi** on X-tee baasprotokollistikul tuginev turvaserveri osa, kuhu salvestatakse e-templiga kinnitatud X-teel vahetatud sõnumid.

**Infolog?** -

**Auditlog** (*halduslogi*) – log, where the user actions (through user interface), when the user changes the system state or configuration, are logged regardless of whether the outcome was a success or failure.

**Batch signature** (*pakkallkiri*) – e-stamp provided to a set of documents, enabling to separate a single document from the set and verify its signature.

### 6.7 Identificators and codes

**X-Road instance identifier** (*X-tee keskkonna identifikaator*) – identifier, that uniquely identifies the X-road instance in the X-Road Network.

**Security server identifier** (*turvaserveri identifikaator*) – identifier, that uniquely identifies security server in X-road Network. The security server identifier consists of security server owner identifier and security server code.

**Security server code** (*turvaserveri kood*) – identifier, that uniquely identifies the security server in all of the security servers of the security server owner.

**Member class** (*X-tee liikmeklass*) - identifier, that is identified by X-Road governing authority and that uniquely identifies members with similar characteristics. Member class must assure the unique member code for each member in member class.

**Member code** (*liikmekood*) – part of member identifier, that uniquely identifies X-Road member in its memberclass. Member code cannot change for the member.

**Member identifier** (*liikme identifikaator*) - identifier, that uniquely identifies member in X-Road Network. Member identifier consists of X-Road instance identifier, member class and member code.

**Subsystem identifier** (*alamsüsteemi identifikaator*) - identifier, that uniquely identifies subsystem in X-Road Network. Subsystem identifier consists of member identifier and subsystem code.

**Subsystem code** (*alamsüsteemi kood*) – code, that uniquely identifies subsystem in all of the subsystems of the member.

**Service identifier** (*teenuse identifikaator*) - identifier, that uniquely identifies service in X-Road Network. The service identifier consists of member identifier of the service provider, service code and version of the service. Including version of the service in the service identifier is optional.

**Central service identifier** (*keskse teenuse identifikaator*) - identifier, that uniquely identifies service in X-Road network wihtout having a reference for service provider. Central service identifier consists of X-Road instance identifier and central service code.

**Global access group identifier** (*globaalse pääsuõiguste grupi identifikaator*) – identifier, that uniquely identifies access group in X-Road Network. Global access group identifier consists of X-Road instance identifier and global group code.

**Local access group identifier** (*lokaalse pääsuõiguste grupi identifikaator*) – identifier, that uniquely identifies access group for a security server client. Global access group identifier consists of X-Road instance identifier and global group code.

