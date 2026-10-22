import { Navigate, Routes, Route } from 'react-router-dom'
import SearchFilter from './components/busqueda/SearchFilter'
import NosotrosPage from './pages/NosotrosPage'
import ContactoPage from './pages/ContactoPage'
import Footer from './components/layout/Footer'
import Navbar from './components/layout/Navbar'
import ScrollToTop from './components/ScrollToTop'
import HeroSection from './components/home/HeroSection'
import OffersSection from './components/home/OffersSection'
import AssistantWidget from './components/chat/AssistantWidget'

// ─── App ─────────────────────────────────────────────────────────────────────

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Navbar />
      <main>
        <Routes>
          {/* Ruta principal (Home */}
          <Route 
            path="/" 
            element={
              <>
                <HeroSection />
                <OffersSection />
                
              </>
            } 
          />

          {/* Ruta para la búsqueda y filtros  */}
          <Route path="/buscar" element={<SearchFilter />} />
          <Route path="/nosotros" element={<NosotrosPage />} />
        {/* Servicios ahora es una parte de Nosotros: los enlaces viejos llevan directo a ella */}
        <Route path="/servicios" element={<Navigate to="/nosotros#servicios" replace />} />
        <Route path="/contacto" element={<ContactoPage/>} />
        <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      {/* Footer visible en todas las páginas */}
      <Footer />

      {/* Asistente flotante, visible en todas las páginas */}
      <AssistantWidget />
    </>
  );
}

