# WebSocket Client Example with JWT Authentication

Этот документ показывает, как подключиться к WebSocket с JWT токеном на фронтенде.

## Подключение с использованием SockJS и STOMP

### JavaScript/TypeScript пример

```javascript
import SockJS from 'sockjs-client';
import { Client, over } from 'stompjs';

// Получить JWT токен из localStorage или другого хранилища
const token = localStorage.getItem('accessToken');

// Создать SockJS соединение
const socket = new SockJS('http://localhost:8080/ws');

// Создать STOMP клиент
const stompClient = over(socket);

// Подключиться с JWT токеном в заголовках
stompClient.connect(
    {
        // Способ 1: Использовать заголовок Authorization
        Authorization: `Bearer ${token}`,
        
        // Способ 2: Или использовать заголовок token (альтернатива)
        // token: token
    },
    // Callback при успешном подключении
    function(frame) {
        console.log('Connected: ' + frame);
        
        // Подписаться на топики
        subscribeToTopics(stompClient);
    },
    // Callback при ошибке
    function(error) {
        console.error('Connection error: ' + error);
        // Обработка ошибки аутентификации
        if (error.headers && error.headers['message']) {
            console.error('Auth error: ' + error.headers['message']);
        }
    }
);

// Функция для подписки на топики
function subscribeToTopics(client) {
    // Подписаться на события сессии
    const sessionId = 'your-session-id'; // UUID сессии
    
    // События конкретной сессии
    client.subscribe(`/topic/sessions/${sessionId}`, function(message) {
        const event = JSON.parse(message.body);
        console.log('Session event:', event);
        handleSessionEvent(event);
    });
    
    // События отчета
    client.subscribe(`/topic/sessions/${sessionId}/report`, function(message) {
        const event = JSON.parse(message.body);
        console.log('Report event:', event);
        handleReportEvent(event);
    });
    
    // События пользователя
    const userId = 'your-user-id'; // UUID пользователя
    client.subscribe(`/topic/users/${userId}/sessions`, function(message) {
        const event = JSON.parse(message.body);
        console.log('User session event:', event);
        handleUserSessionEvent(event);
    });
}

// Обработчики событий
function handleSessionEvent(event) {
    switch(event.type) {
        case 'SESSION_CREATED':
            console.log('Session created:', event.data);
            break;
        case 'SESSION_UPDATED':
            console.log('Session updated:', event.data);
            break;
        case 'PARTICIPANT_JOINED':
            console.log('Participant joined:', event.data);
            break;
        case 'PARTICIPANT_LEFT':
            console.log('Participant left:', event.data);
            break;
        case 'SESSION_ENDED':
            console.log('Session ended:', event.data);
            break;
        case 'REPORT_READY':
            console.log('Report ready:', event.data);
            break;
    }
}

function handleReportEvent(event) {
    if (event.type === 'REPORT_READY') {
        console.log('Report is ready:', event.data);
        // Обновить UI с данными отчета
    }
}

function handleUserSessionEvent(event) {
    // Обработка событий сессий пользователя
    console.log('User session event:', event);
}

// Отключение
function disconnect() {
    if (stompClient && stompClient.connected) {
        stompClient.disconnect();
        console.log('Disconnected');
    }
}
```

### React Hook пример

```typescript
import { useEffect, useRef, useState } from 'react';
import SockJS from 'sockjs-client';
import { Client, over, IMessage } from 'stompjs';

interface WebSocketEvent {
    type: string;
    data: any;
}

export function useWebSocket(token: string | null, sessionId: string | null, userId: string | null) {
    const [connected, setConnected] = useState(false);
    const [events, setEvents] = useState<WebSocketEvent[]>([]);
    const stompClientRef = useRef<Client | null>(null);

    useEffect(() => {
        if (!token || !sessionId || !userId) {
            return;
        }

        // Создать соединение
        const socket = new SockJS('http://localhost:8080/ws');
        const client = over(socket);
        stompClientRef.current = client;

        // Подключиться
        client.connect(
            {
                Authorization: `Bearer ${token}`
            },
            () => {
                console.log('WebSocket connected');
                setConnected(true);

                // Подписаться на топики
                if (sessionId) {
                    // События сессии
                    client.subscribe(`/topic/sessions/${sessionId}`, (message: IMessage) => {
                        const event = JSON.parse(message.body);
                        setEvents(prev => [...prev, event]);
                    });

                    // События отчета
                    client.subscribe(`/topic/sessions/${sessionId}/report`, (message: IMessage) => {
                        const event = JSON.parse(message.body);
                        setEvents(prev => [...prev, event]);
                    });
                }

                // События пользователя
                if (userId) {
                    client.subscribe(`/topic/users/${userId}/sessions`, (message: IMessage) => {
                        const event = JSON.parse(message.body);
                        setEvents(prev => [...prev, event]);
                    });
                }
            },
            (error) => {
                console.error('WebSocket connection error:', error);
                setConnected(false);
            }
        );

        // Cleanup при размонтировании
        return () => {
            if (client && client.connected) {
                client.disconnect();
            }
        };
    }, [token, sessionId, userId]);

    return { connected, events };
}

// Использование в компоненте
function SessionComponent() {
    const token = localStorage.getItem('accessToken');
    const sessionId = 'your-session-id';
    const userId = 'your-user-id';
    
    const { connected, events } = useWebSocket(token, sessionId, userId);

    useEffect(() => {
        events.forEach(event => {
            if (event.type === 'REPORT_READY') {
                console.log('Report ready!', event.data);
            }
        });
    }, [events]);

    return (
        <div>
            <p>WebSocket: {connected ? 'Connected' : 'Disconnected'}</p>
            {/* Ваш UI */}
        </div>
    );
}
```

### Vue.js Composition API пример

```typescript
import { ref, onMounted, onUnmounted } from 'vue';
import SockJS from 'sockjs-client';
import { Client, over } from 'stompjs';

export function useWebSocket(token: string, sessionId: string, userId: string) {
    const connected = ref(false);
    const events = ref<any[]>([]);
    let stompClient: Client | null = null;

    const connect = () => {
        const socket = new SockJS('http://localhost:8080/ws');
        stompClient = over(socket);

        stompClient.connect(
            { Authorization: `Bearer ${token}` },
            () => {
                connected.value = true;
                console.log('WebSocket connected');

                // Подписки
                stompClient!.subscribe(`/topic/sessions/${sessionId}`, (message) => {
                    const event = JSON.parse(message.body);
                    events.value.push(event);
                });

                stompClient!.subscribe(`/topic/sessions/${sessionId}/report`, (message) => {
                    const event = JSON.parse(message.body);
                    events.value.push(event);
                });

                stompClient!.subscribe(`/topic/users/${userId}/sessions`, (message) => {
                    const event = JSON.parse(message.body);
                    events.value.push(event);
                });
            },
            (error) => {
                console.error('WebSocket error:', error);
                connected.value = false;
            }
        );
    };

    const disconnect = () => {
        if (stompClient && stompClient.connected) {
            stompClient.disconnect();
            connected.value = false;
        }
    };

    onMounted(() => {
        connect();
    });

    onUnmounted(() => {
        disconnect();
    });

    return { connected, events, disconnect };
}
```

## Обработка ошибок аутентификации

Если JWT токен невалиден или отсутствует, WebSocket соединение будет отклонено с ошибкой:

```javascript
stompClient.connect(
    { Authorization: `Bearer ${token}` },
    onConnect,
    (error) => {
        if (error.headers && error.headers['message']) {
            // Ошибка аутентификации
            console.error('Auth failed:', error.headers['message']);
            // Перенаправить на страницу логина или обновить токен
            handleAuthError();
        } else {
            // Другая ошибка
            console.error('Connection failed:', error);
        }
    }
);
```

## Обновление токена

Если токен истекает, нужно переподключиться с новым токеном:

```javascript
function refreshConnection() {
    disconnect();
    const newToken = getNewToken(); // Получить новый токен
    connect(newToken);
}
```

## Важные замечания

1. **Токен должен быть валидным**: JWT токен проверяется при каждом подключении
2. **CORS**: Убедитесь, что CORS настроен правильно для вашего фронтенда
3. **HTTPS в продакшене**: Используйте `wss://` вместо `ws://` в продакшене
4. **Обработка переподключения**: Реализуйте логику автоматического переподключения при разрыве соединения

