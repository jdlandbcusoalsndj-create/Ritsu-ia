# Ritsu AI Assistant

![Ritsu Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## 🌟 Descripción

**Ritsu AI Assistant** es una aplicación Android completa que presenta un asistente de IA avatar 3D que vive en tu teléfono. Ritsu puede manejar llamadas telefónicas, responder mensajes, controlar aplicaciones y mucho más, ¡todo completamente **GRATIS** y sin límites!

### ✨ Características Principales

#### 🎭 Avatar 3D Interactivo
- **Diseño**: Personaje anime femenino con estilo Live2D/VTuber
- **Animaciones**: Movimientos fluidos, parpadeo, expresiones faciales
- **Interactividad**: Responde al toque, se puede arrastrar por la pantalla
- **Overlay**: Aparece sobre cualquier aplicación cuando se activa

#### 🧠 Sistema de IA 100% Gratuito
- **Motor Local**: LLaMA 2/3 (7B) corriendo on-device
- **Sin Costos**: Sin APIs de pago, sin suscripciones, sin límites
- **Personalidad**: Amigable, útil, con toque de personalidad anime
- **Memoria**: Conversaciones y preferencias guardadas localmente
- **Idiomas**: Español, inglés, japonés básico

#### 📞 Llamadas Telefónicas Conversacionales
- **Auto-respuesta**: Ritsu contesta llamadas automáticamente
- **Conversación Real**: Mantiene diálogos naturales con los llamantes
- **IA Contextual**: Comprende y responde según la situación
- **Filtro Inteligente**: Detecta y rechaza spam automáticamente
- **Transcripción**: Guarda resúmenes de todas las conversaciones

#### 💬 Mensajería Inteligente
- **Multi-plataforma**: SMS, WhatsApp, Telegram
- **Respuestas Automáticas**: Generadas por IA en tiempo real
- **Lectura en Voz Alta**: Lee mensajes entrantes
- **Filtro de Spam**: Identifica y bloquea mensajes no deseados

#### 🎤 Reconocimiento de Voz Offline
- **Wake Word**: Activación con "Hey Ritsu"
- **Completamente Offline**: Funciona sin conexión a internet
- **Comandos Naturales**: Habla normalmente, no necesitas frases específicas
- **TTS Gratuito**: Síntesis de voz usando tecnologías open source

#### 🏠 Launcher Personalizado
- **Reemplazo Completo**: Sustituye el launcher por defecto
- **Ritsu Central**: Avatar en el centro de la pantalla principal
- **Iconos Inteligentes**: Aplicaciones organizadas automáticamente
- **Widgets Interactivos**: Clima, hora, notificaciones

### 🛠 Tecnologías Utilizadas

#### Frontend
- **Kotlin** - Lenguaje principal
- **Jetpack Compose** - UI moderna y reactiva
- **Material Design 3** - Diseño consistente y accesible
- **OpenGL ES** - Renderizado 3D del avatar

#### Backend & AI
- **Room Database** - Base de datos local
- **ONNX Runtime** - Inferencia de modelos IA
- **Mozilla DeepSpeech** - Reconocimiento de voz offline
- **eSpeak-NG** - Síntesis de voz gratuita
- **Coroutines** - Programación asíncrona

#### Servicios del Sistema
- **Accessibility Service** - Interacción con otras apps
- **Notification Listener** - Manejo de notificaciones
- **Telecom Framework** - Manejo de llamadas
- **Overlay Service** - Avatar flotante

### 📱 Requisitos del Sistema

- **Android 8.0+** (API level 26+)
- **4GB RAM** mínimo (recomendado 6GB+)
- **2GB espacio libre** para modelos de IA
- **Micrófono** (requerido)
- **Cámara** (opcional pero recomendado)

### 🚀 Instalación

#### Desde Código Fuente
```bash
# Clonar el repositorio
git clone https://github.com/tuusuario/ritsu-ai-assistant.git

# Abrir en Android Studio
cd ritsu-ai-assistant

# Compilar y ejecutar
./gradlew assembleDebug
```

#### Configuración Inicial
1. **Permisos**: La app solicitará permisos necesarios
2. **Overlay**: Permitir mostrar sobre otras apps
3. **Accesibilidad**: Habilitar servicio de accesibilidad
4. **Launcher**: Configurar como launcher por defecto
5. **IA Local**: Descarga automática de modelos (opcional)

### 📖 Uso

#### Activación Básica
- **Comando de Voz**: "Hey Ritsu"
- **Toque**: Toca el avatar en pantalla
- **Gesto**: Desliza desde el borde derecho

#### Comandos de Ejemplo
```
"Hey Ritsu, abre WhatsApp"
"Llama a mamá"
"¿Cómo está el clima?"
"Lee mis mensajes"
"Responde que llego en 10 minutos"
"Silencio por una hora"
```

#### Manejo de Llamadas
- Ritsu preguntará si debe contestar llamadas entrantes
- Mantiene conversaciones naturales con los llamantes
- Toma mensajes cuando no estés disponible
- Filtra automáticamente llamadas de spam

### ⚙️ Configuración

#### Preferencias Principales
- **Auto-respuesta**: Configurar para llamadas y mensajes
- **Horarios**: Definir cuándo Ritsu debe estar activa
- **Personalidad**: Ajustar el tono de las respuestas
- **Privacidad**: Controlar qué información puede compartir

#### Configuración Avanzada
- **Modelos IA**: Seleccionar modelos locales
- **Reconocimiento**: Calibrar sensibilidad de voz
- **Filtros**: Configurar detección de spam
- **Integración**: Conectar con apps específicas

### 🔒 Privacidad y Seguridad

#### Datos Locales
- **100% Local**: Toda la IA funciona en tu dispositivo
- **Sin Telemetría**: No se envían datos a servidores externos
- **Cifrado**: Base de datos local cifrada
- **Control Total**: Tú decides qué datos guardar

#### Permisos Mínimos
- Solo solicita permisos estrictamente necesarios
- Explicación clara del uso de cada permiso
- Opción de usar funciones básicas sin permisos opcionales

### 🤝 Contribuir

¡Las contribuciones son bienvenidas! Por favor:

1. Haz fork del proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

### 📝 Roadmap

#### Versión 1.1
- [ ] Integración con más apps de mensajería
- [ ] Comandos de automatización del hogar
- [ ] Widget de escritorio mejorado
- [ ] Soporte para más idiomas

#### Versión 1.2
- [ ] Sistema de plugins
- [ ] Integración con calendarios
- [ ] Recordatorios inteligentes
- [ ] Modo conducción

#### Versión 2.0
- [ ] Avatar 3D mejorado con Live2D
- [ ] Gestos y expresiones más realistas
- [ ] IA conversacional avanzada
- [ ] Integración con IoT

### 🐛 Reportar Problemas

Si encuentras algún problema:

1. Verifica que tu dispositivo cumple los requisitos mínimos
2. Revisa los [Issues](https://github.com/tuusuario/ritsu-ai-assistant/issues) existentes
3. Crea un nuevo Issue con:
   - Modelo de dispositivo
   - Versión de Android
   - Pasos para reproducir el problema
   - Logs relevantes

### 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

### 🙏 Agradecimientos

- **Comunidad Open Source** por las tecnologías base
- **Mozilla** por DeepSpeech
- **ONNX** por el runtime de IA
- **Material Design** por las guías de diseño
- **Anime Community** por la inspiración del personaje

### 💬 Contacto

- **GitHub**: [Issues](https://github.com/tuusuario/ritsu-ai-assistant/issues)
- **Email**: support@ritsu-ai.com
- **Discord**: RitsuAI#1234

---

**¡Hecho con ❤️ para la comunidad Android!**

*Ritsu AI Assistant - Tu compañera virtual completamente gratuita*