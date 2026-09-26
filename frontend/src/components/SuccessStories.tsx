import styles from './SuccessStories.module.css'

const STORIES = [
  {
    name: 'Martina Rodríguez',
    property: 'Casa en Area 1',
    location: 'Ciudad del Este, Area 1',
    date: 'Ene 2026',
    quote: 'Después de meses buscando, encontramos el hogar que siempre soñamos.',
    avatar: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200&h=200&fit=crop&auto=format',
    bg: './images/propiedades/prop-001/1.png',
    featured: true,
  },
  {
    name: 'Lucas Pereyra',
    property: 'Casa en Minga',
    location: 'Minga Guazu, Virgen del Carmen',
    date: 'Mar 2026',
    quote: 'La herramienta de mapa por barrios me ahorró semanas de búsqueda. Increíble experiencia.',
    avatar: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&h=200&fit=crop&auto=format',
    bg: './images/propiedades/prop-011/1.png',
    featured: false,
  },
  {
    name: 'Valentina Gómez',
    property: 'Depa en el Country',
    location: 'Hernandarias, Parana Country Club',
    date: 'Dic 2025',
    quote: 'Viviendo en el Country como siempre soñé, gracias S&N',
    avatar: 'https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200&h=200&fit=crop&auto=format',
    bg: './images/propiedades/prop-013/1.png',
    featured: false,
  },
   {
    name: 'Leandro Gimenez',
    property: 'Casa en San Miguel',
    location: 'Franco, San Miguel',
    date: 'Dic 2025',
    quote: 'Nunca pensé conseguir la casa de mis sueños tan facil !',
    avatar: 'https://i.pinimg.com/736x/2f/ab/67/2fab6751b7ff2d24a423b32cf0eee4e2.jpg',
    bg: './images/propiedades/prop-016/1.png',
    featured: false,
  },
  
]

export default function SuccessStories() {
  return (
    <section className={styles.section}>
      <div className={styles.container}>
        {/* Header */}
        <div className={styles.header}>
          <div>
            
            <h2 className={styles.title}>
              Personas que encontraron
              <br />
              <em className={styles.titleAccent}>el hogar de sus sueños.</em>
            </h2>
          </div>
          <div className={styles.headerRight}>
            <p className={styles.headerSub}>
              Más de 8.400 familias confiaron en S&N para el momento más importante de sus vidas.
            </p>
            <div className={styles.stats}>
              <div className={styles.stat}>
                <span className={styles.statValue}>8.4K</span>
                <span className={styles.statLabel}>Clientes felices</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statValue}>97%</span>
                <span className={styles.statLabel}>Satisfacción</span>
              </div>
              <div className={styles.stat}>
                <span className={styles.statValue}>18 días</span>
                <span className={styles.statLabel}>Cierre promedio</span>
              </div>
            </div>
          </div>
        </div>

        {/* Grid — first story spans 2 columns */}
        <div className={styles.grid}>
          {STORIES.map(story => (
            <StoryCard key={story.name} story={story} />
          ))}
        </div>
      </div>
    </section>
  )
}

function StoryCard({ story }: { story: (typeof STORIES)[0] }) {
  return (
    <div className={`${styles.card} ${story.featured ? styles.cardFeatured : ''}`}>
      <img className={styles.cardImg} src={story.bg} alt={story.property} />
      <div className={styles.cardOverlay} />
      <div className={styles.cardBody}>
        <div className={styles.cardAvatar}>
          <img className={styles.avatarImg} src={story.avatar} alt={story.name} />
          <div className={styles.avatarInfo}>
            <span className={styles.avatarName}>{story.name}</span>
            <span className={styles.avatarProp}>{story.property}</span>
          </div>
        </div>
        <p className={styles.quote}>
          <span className={styles.quoteOpen}>"</span>
          {story.quote}
        </p>
        <div className={styles.cardFooter}>
          <span className={styles.cardLocation}>📍 {story.location}</span>
          <span className={styles.cardDate}>{story.date}</span>
        </div>
      </div>
    </div>
  )
}
