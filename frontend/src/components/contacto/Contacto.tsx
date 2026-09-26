import styles from './Contacto.module.css';

// Lista de agentes (podés agregar o editar los datos según necesites)
const AGENTES = [
  {
    id: 1,
    nombre: 'Sonia Giménez',
    cargo: 'Asesora Inmobiliaria Senior',
    telefono: '+595 981 123 456',
    email: 'sonia.gimenez@inmobiliaria.com',
    foto: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=400&q=80',
    especialidad: 'Ventas Residenciales & Terrenos',
  },
  {
    id: 2,
    nombre: 'Néstor Benítez',
    cargo: 'Especialista en Inversiones',
    telefono: '+595 982 654 321',
    email: 'nestor.benitez@inmobiliaria.com',
    foto: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&w=400&q=80',
    especialidad: 'Desarrollos Comerciales & Alquileres',
  },
  {
    id: 3,
    nombre: 'Carolina Ríos',
    cargo: 'Agente Inmobiliaria',
    telefono: '+595 983 987 654',
    email: 'carolina.rios@inmobiliaria.com',
    foto: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=400&q=80',
    especialidad: 'Propiedades de Lujo',
  },
];

export default function Contacto() {
  return (
    <section className={styles.container}>
      <h1 className={styles.title}>Nuestros Agentes</h1>
      <p className={styles.subtitle}>
        Ponte en contacto directamente con cualquiera de nuestros asesores para recibir atención personalizada.
      </p>

      <div className={styles.grid}>
        {AGENTES.map((agente) => {
          // Prepara el enlace para abrir WhatsApp Web/App directamente
          const numWa = agente.telefono.replace(/\s+/g, '').replace('+', '');
          const waUrl = `https://wa.me/${numWa}?text=Hola%20${encodeURIComponent(agente.nombre)},%20quisiera%20recibir%20información%20sobre%20sus%20propiedades.`;

          return (
            <div key={agente.id} className={styles.card}>
              <img src={agente.foto} alt={agente.nombre} className={styles.avatar} />
              <h3 className={styles.name}>{agente.nombre}</h3>
              <span className={styles.role}>{agente.cargo}</span>
              <p className={styles.specialty}>📍 {agente.especialidad}</p>
              
              <div className={styles.info}>
                <p>📞 {agente.telefono}</p>
                <p>✉️ {agente.email}</p>
              </div>

              <a
                href={waUrl}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.button}
              >
                Contactar por WhatsApp
              </a>
            </div>
          );
        })}
      </div>
    </section>
  );
}