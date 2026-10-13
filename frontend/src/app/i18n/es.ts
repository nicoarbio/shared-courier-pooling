/**
 * Spanish (Argentina) UI texts. Every visible text lives here under a key; templates use `'key' | t`.
 * To add a language, create another file with the same shape (`Messages`) and register it in translate.ts.
 * `{name}` placeholders are replaced with the params passed to the pipe.
 */
export const es = {
  app: {
    title: 'Delivery colaborativo',
  },
  home: {
    question: '¿Cómo querés usar la app?',
    merchant: 'Soy comercio',
    merchantHint: 'Cargá pedidos y mirá en qué ronda salen y cuánto pagás.',
    courier: 'Soy repartidor',
    courierHint: 'Mirá las rondas listas para salir y su recorrido.',
  },
  merchant: {
    select: {
      title: '¿Qué comercio sos?',
      change: 'Cambiar de comercio',
    },
    nav: {
      newOrder: 'Nuevo pedido',
      orders: 'Mis pedidos',
    },
    newOrder: {
      title: 'Nuevo pedido',
      addressLabel: 'Dirección de entrega',
      addressHint: 'Calle y altura, por ejemplo: Yerbal 1550',
      searching: 'Buscando…',
      noResults: 'No encontramos esa dirección cerca del comercio.',
      noHouseNumber: 'No encontramos la altura exacta: ubicá el punto en el mapa.',
      mapHint: 'Revisá que el punto esté bien ubicado. Podés arrastrarlo.',
      windowStart: 'Entregar desde',
      windowEnd: 'Hasta',
      windowInvalid: 'El fin de la ventana tiene que ser posterior al inicio.',
      submit: 'Cargar pedido',
      submitting: 'Cargando…',
      done: 'Pedido cargado',
      doneRound: 'Sale en una ronda compartida. Entrega estimada: {time}.',
      doneShare: 'Tu comercio paga {amount} por esta ronda.',
      another: 'Cargar otro',
    },
    orders: {
      title: 'Mis pedidos',
      empty: 'Todavía no cargaste pedidos.',
      window: 'Ventana {start} – {end}',
      estimatedDelivery: 'Entrega estimada {time}',
      share: 'Tu comercio paga {amount} por la ronda',
      status: {
        PENDING: 'Pendiente',
        PLANNED: 'En ronda',
        DISPATCHED: 'En camino',
      },
    },
  },
  courier: {
    rounds: {
      title: 'Rondas para salir',
      empty: 'No hay rondas para salir.',
      summary: '{deliveries} entregas · {km} km · sale {time}',
      open: 'Ver ronda',
    },
    round: {
      title: 'Ronda',
      back: 'Volver a las rondas',
      summary: '{km} km · sale {departure} · vuelve {return}',
      cost: 'Costo de la ronda: {amount}',
      stops: 'Paradas',
      stopKind: {
        PICKUP: 'Retirar en',
        DELIVERY: 'Entregar',
        RETURN: 'Volver a',
      },
      shares: 'Reparto del costo',
      openMaps: 'Abrir en Google Maps',
      noMaps: 'La ronda tiene demasiadas paradas para abrirla en Google Maps.',
      dispatch: 'Salir con esta ronda',
      dispatched: 'Ronda en camino',
    },
  },
  common: {
    refresh: 'Actualizar',
    loading: 'Cargando…',
    retry: 'Reintentar',
  },
  errors: {
    network: 'No pudimos conectarnos con el servidor.',
    notFound: 'No encontramos lo que buscabas.',
    conflict: 'La operación ya no es válida: actualizá la pantalla.',
    invalid: 'Revisá los datos ingresados.',
    unavailable: 'Un servicio necesario no está disponible. Probá de nuevo en un rato.',
    unexpected: 'Ocurrió un error inesperado.',
    requestId: 'Código de error: {id}',
  },
};

export type Messages = typeof es;
