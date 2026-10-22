import { describe, it, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup, act, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import App from '../src/App'
import { ProveedorSesion } from '../src/auth/Sesion'
import { fetchFalso, pedidos, estado, db, conRol, usuariosDb } from './backendFalso'
import * as falso from './backendFalso'
import { readFileSync } from 'node:fs'
import { saludoDeLaHora } from '../src/paginas/Inicio'
import { porcentajes, Torta, Medidor } from '../src/componentes/fichas'

let clicEnMapa: ((e: { latlng: { lat: number; lng: number } }) => void) | null = null
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  TileLayer: () => null, GeoJSON: () => null,
  Marker: ({ position }: { position: [number, number] }) => <div data-testid="marcador">{position.join(',')}</div>,
  useMapEvents: (h: { click: (e: { latlng: { lat: number; lng: number } }) => void }) => { clicEnMapa = h.click; return null },
}))

const abrir = (ruta: string) => render(<MemoryRouter initialEntries={[ruta]}><ProveedorSesion><App /></ProveedorSesion></MemoryRouter>)
const ultimo = (metodo: string, parte: string) => [...pedidos].reverse().find((p) => p.metodo === metodo && p.url.includes(parte))
const campo = (etiqueta: string | RegExp) => screen.getByText(etiqueta).parentElement!.querySelector('input, select, textarea') as HTMLInputElement
async function clicMapa(lat: number, lng: number) { await waitFor(() => expect(clicEnMapa).not.toBeNull()); await act(async () => clicEnMapa!({ latlng: { lat, lng } })) }

beforeEach(() => {
  cleanup(); conRol('ADMIN'); localStorage.clear(); delete document.documentElement.dataset.tema; pedidos.length = 0; estado.sesion = true; estado.expirarEnListado = false; estado.errorAlCrear = null; clicEnMapa = null
  db.lista = db.lista.map((x) => ({ ...x, estado: 'Disponible' }))
  db.detalle4 = { ...db.detalle4, titulo: 'Departamento en San Jose' }
  vi.stubGlobal('fetch', vi.fn(fetchFalso)); vi.stubGlobal('confirm', vi.fn(() => true)); window.scrollTo = vi.fn() as any
})

describe('Sesión y protección', () => {
  it('sin sesión, una ruta protegida lleva al login', async () => {
    estado.sesion = false; abrir('/propiedades')
    expect(await screen.findByText('Ingresar')).toBeTruthy()
  })
  it('credenciales incorrectas muestran el mensaje del backend; correctas abren Inicio', async () => {
    estado.sesion = false; abrir('/login')
    fireEvent.change(await screen.findByLabelText(/^Email/), { target: { value: 'admin@sn.local' } })
    fireEvent.change(screen.getByLabelText(/Contraseña/), { target: { value: 'mal' } }); fireEvent.click(screen.getByText('Ingresar'))
    expect(await screen.findByText('Email o contraseña incorrectos')).toBeTruthy()
    fireEvent.change(screen.getByLabelText(/Contraseña/), { target: { value: 'Clave-Correcta' } }); fireEvent.click(screen.getByText('Ingresar'))
    expect(await screen.findByRole('heading', { name: /^Buen(os|as) (días|tardes|noches), Paula$/ })).toBeTruthy()
  })
  it('si el backend responde 401 durante el uso, vuelve al login avisando que la sesión expiró', async () => {
    estado.expirarEnListado = true; abrir('/propiedades')
    expect(await screen.findByText('Su sesión expiró. Vuelva a iniciar sesión.')).toBeTruthy()
  })
  it('cerrar sesión llama al backend y vuelve al login', async () => {
    abrir('/propiedades'); fireEvent.click(await screen.findByText('Cerrar sesión'))
    expect(await screen.findByText('Ingresar')).toBeTruthy(); expect(ultimo('POST', '/api/admin/auth/logout')).toBeTruthy()
  })
})

describe('Listado', () => {
  it('muestra las propiedades y filtra por texto y tipo', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004'); expect(screen.getByText('terr-001')).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: 'Terreno' } }); expect(screen.queryByText('prop-004')).toBeNull()
    fireEvent.change(screen.getByLabelText('Tipo'), { target: { value: '' } }); fireEvent.change(screen.getByLabelText('Buscar'), { target: { value: 'san jose' } })
    expect(screen.getByText('prop-004')).toBeTruthy(); expect(screen.queryByText('terr-001')).toBeNull()
  })
  it('"Dar de baja" usa la baja lógica: DELETE y la propiedad queda Inactivo, sin desaparecer', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004'); fireEvent.click(screen.getAllByText('Dar de baja')[0])
    expect(await screen.findByText('prop-004 quedó como Inactivo.')).toBeTruthy(); expect(ultimo('DELETE', '/api/admin/propiedades/4')).toBeTruthy()
    expect(screen.getByText('prop-004')).toBeTruthy(); expect(screen.getAllByText('Inactivo').length).toBeGreaterThan(0)
  })
})

describe('Formulario: operaciones y ubicación', () => {
  it('un terreno solo ofrece Venta; una casa puede tener Venta y Alquiler', async () => {
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda')
    fireEvent.click(screen.getByText('+ Agregar Alquiler')); expect(screen.getByLabelText('Operación 2')).toBeTruthy()
    fireEvent.change(campo('Tipo'), { target: { value: 'Terreno' } }); expect(await screen.findByText('Datos del terreno')).toBeTruthy()
    expect(screen.queryByLabelText('Operación 2')).toBeNull()
    expect([...(screen.getByLabelText('Operación 1') as HTMLSelectElement).options].map((o) => o.value)).toEqual(['Venta'])
    expect(screen.getByText('Un terreno solamente puede ponerse en venta.')).toBeTruthy()
  })
  it('el barrio, la ciudad y el departamento los determina /api/admin/ubicacion', async () => {
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda'); await clicMapa(-25.5117004, -54.6118)
    expect(await screen.findByText('AREA 1')).toBeTruthy(); expect(screen.getByText('ALTO PARANÁ')).toBeTruthy()
    expect(ultimo('GET', '/api/admin/ubicacion')!.url).toContain('latitud=-25.511700&longitud=-54.611800')
  })
  it('un punto fuera de las zonas muestra el aviso y NO permite guardar', async () => {
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda'); await clicMapa(0, 0)
    expect(await screen.findByText('El punto marcado está fuera de las zonas registradas.')).toBeTruthy()
    fireEvent.change(campo('Título'), { target: { value: 'Casa X' } }); fireEvent.click(screen.getByText('Crear propiedad'))
    expect(await screen.findByText(/No se puede guardar: Marque en el mapa un punto dentro/)).toBeTruthy(); expect(ultimo('POST', '/api/admin/propiedades')).toBeUndefined()
  })
  it('crear: envía el barrio detectado y los datos de la casa, y pasa a la edición para agregar fotos', async () => {
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda')
    fireEvent.change(campo('Título'), { target: { value: 'Casa nueva' } }); fireEvent.change(campo('Habitaciones'), { target: { value: '3' } })
    fireEvent.change(campo('Baños'), { target: { value: '2' } }); fireEvent.change(campo('Superficie construida (m²)'), { target: { value: '120' } })
    fireEvent.change(screen.getByLabelText('Precio 1'), { target: { value: '80000' } }); await clicMapa(-25.51, -54.61); await screen.findByText('AREA 1')
    fireEvent.click(screen.getByText('Crear propiedad'))
    expect(await screen.findByText('Propiedad prop-017 creada. Ahora puede agregar las fotos.')).toBeTruthy()
    expect(await screen.findByText('Editar prop-017')).toBeTruthy()
    const cuerpo = ultimo('POST', '/api/admin/propiedades')!.cuerpo as any
    expect(cuerpo.idBarrio).toBe(10011013); expect(cuerpo.casa).toMatchObject({ habitaciones: 3, banos: 2, superficieConstruida: 120 })
    expect(cuerpo.operaciones).toEqual([{ operacion: 'Venta', precio: 80000, moneda: 'PYG' }])
  })
  it('los errores del backend aparecen junto a cada campo', async () => {
    estado.errorAlCrear = { titulo: 'El título es obligatorio', 'operaciones[0].precio': 'Obligatorio', 'otro.campo': 'Detalle extra' }
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda'); await clicMapa(-25.51, -54.61); await screen.findByText('AREA 1')
    fireEvent.click(screen.getByText('Crear propiedad'))
    expect(await screen.findByText('El título es obligatorio')).toBeTruthy(); expect(screen.getByText('Obligatorio')).toBeTruthy(); expect(screen.getByText('Detalle extra')).toBeTruthy()
  })
})

describe('Edición e imágenes', () => {
  it('corregir el título "Departamento en…" envía el cambio con PUT', async () => {
    abrir('/propiedades/4'); const titulo = await waitFor(() => campo('Título')); expect(titulo.value).toBe('Departamento en San Jose')
    fireEvent.change(titulo, { target: { value: 'Casa en San Jose' } }); fireEvent.click(screen.getByText('Guardar cambios'))
    expect(await screen.findByText('Cambios guardados.')).toBeTruthy(); expect((ultimo('PUT', '/api/admin/propiedades/4')!.cuerpo as any).titulo).toBe('Casa en San Jose')
  })
  it('imágenes: avisa antes de subir archivos no admitidos o de más de 5 MB', async () => {
    abrir('/propiedades/4'); const entrada = (await screen.findByLabelText('Archivo de imagen')) as HTMLInputElement
    fireEvent.change(entrada, { target: { files: [new File(['x'], 'doc.pdf', { type: 'application/pdf' })] } }); fireEvent.click(screen.getByText('Subir foto'))
    expect(await screen.findByText('Formato no admitido. Use imágenes JPEG, PNG o WebP.')).toBeTruthy()
    fireEvent.change(entrada, { target: { files: [new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'grande.jpg', { type: 'image/jpeg' })] } }); fireEvent.click(screen.getByText('Subir foto'))
    expect(await screen.findByText('La imagen supera el máximo de 5 MB.')).toBeTruthy(); expect(ultimo('POST', '/imagenes')).toBeUndefined()
  })
  it('imágenes: subir, hacer principal y eliminar usan los endpoints del backend', async () => {
    abrir('/propiedades/4'); const entrada = (await screen.findByLabelText('Archivo de imagen')) as HTMLInputElement
    fireEvent.change(entrada, { target: { files: [new File(['jpg'], 'foto.jpg', { type: 'image/jpeg' })] } }); fireEvent.click(screen.getByText('Subir foto'))
    await waitFor(() => expect(ultimo('POST', '/api/admin/propiedades/4/imagenes')).toBeTruthy())
    fireEvent.click(await screen.findByText('Hacer principal')); await waitFor(() => expect(ultimo('PATCH', '/imagenes/11/principal')).toBeTruthy())
    fireEvent.click(screen.getAllByText('Eliminar')[1]); await waitFor(() => expect(ultimo('DELETE', '/imagenes/11')).toBeTruthy())
  })
})

// ---------------- Etapa 1: estructura, navegación y Mi cuenta ----------------
describe('Menú lateral y navegación', () => {
  it('muestra las secciones que existen y marca la activa', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    const menu = screen.getByRole('navigation')
    for (const texto of ['Inicio', 'Propiedades', 'Mi cuenta']) expect(within(menu).getByText(texto)).toBeTruthy()
    expect(within(menu).queryByText('Nueva propiedad')).toBeNull()             // está en el listado, como "Nuevo usuario" en Usuarios
    expect(within(menu).getByText('Propiedades').closest('a')!.getAttribute('aria-current')).toBe('page')
    expect(within(menu).getByText('Inicio').closest('a')!.getAttribute('aria-current')).toBeNull()
    expect(within(menu).getAllByRole('link')).toHaveLength(6) // Inicio, Listado, Gestión del sitio web, Consultas IA, Usuarios y Mi cuenta
  })
  it('navega entre secciones desde el menú; "Nueva propiedad" se abre desde el listado', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    fireEvent.click(screen.getByRole('link', { name: 'Nueva propiedad' }))
    expect(await screen.findByRole('heading', { name: 'Nueva propiedad' })).toBeTruthy()
    fireEvent.click(within(screen.getByRole('navigation')).getByText('Inicio'))
    expect(await screen.findByRole('heading', { name: /, Paula$/ })).toBeTruthy()
    fireEvent.click(within(screen.getByRole('navigation')).getByText('Mi cuenta'))
    expect(await screen.findByRole('heading', { name: 'Mi cuenta' })).toBeTruthy()
  })
  it('en pantallas angostas el menú se abre y se cierra con el botón, y se cierra al navegar', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    const boton = screen.getByLabelText('Abrir menú'); expect(boton.getAttribute('aria-expanded')).toBe('false')
    fireEvent.click(boton); expect(screen.getByLabelText('Cerrar menú').getAttribute('aria-expanded')).toBe('true')
    fireEvent.click(within(screen.getByRole('navigation')).getByText('Mi cuenta'))
    await screen.findByRole('heading', { name: 'Mi cuenta' }); expect(screen.getByLabelText('Abrir menú').getAttribute('aria-expanded')).toBe('false')
  })
  it('la marca dice "S&N" + "Inmobiliaria" sin duplicar letras, y muestra el usuario y su rol', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    const lateral = screen.getByLabelText('Menú del panel')
    expect((lateral.textContent!.match(/S&N/g) ?? []).length).toBe(1)
    expect(lateral.textContent).toMatch(/S&NInmobiliaria/); expect(within(lateral).getByText('Paula Admin')).toBeTruthy()
    expect(within(lateral).getByText('Administrador')).toBeTruthy()
  })
})

describe('Mi cuenta', () => {
  it('filas horizontales con el rol primero, y cada dato con su botón de edición', async () => {
    abrir('/cuenta'); await screen.findByText('Información de la cuenta')
    const etiquetas = [...document.querySelectorAll('main span')].map((e) => e.textContent).filter((t) => t?.endsWith(':'))
    expect(etiquetas.slice(0, 5)).toEqual(['Rol:', 'Nombre:', 'Apellido:', 'Email:', 'Contraseña:'])
    expect(within(screen.getByRole('main')).getByText('Administrador')).toBeTruthy()
    expect(screen.getByText('paula@sn.local')).toBeTruthy()
    for (const e of ['Editar nombre', 'Editar apellido', 'Editar email', 'Cambiar contraseña', 'Editar cédula', 'Editar teléfono']) expect(screen.getByLabelText(e)).toBeTruthy()
    expect(screen.queryByLabelText('Editar rol')).toBeNull()                    // el rol no se edita desde la cuenta propia
  })
  it('cerrar sesión sigue disponible en el menú lateral', async () => {
    abrir('/cuenta'); await screen.findByRole('heading', { name: 'Mi cuenta' })
    fireEvent.click(within(screen.getByLabelText('Menú del panel')).getByText('Cerrar sesión'))
    expect(await screen.findByText('Ingresar')).toBeTruthy()
  })
})

// ---------------- Etapa 2A: opciones según el rol ----------------
describe('Opciones según el rol (el backend vuelve a comprobarlo)', () => {
  it('el vendedor ve y crea propiedades, pero no tiene "Dar de baja"', async () => {
    conRol('VENDEDOR'); abrir('/propiedades'); await screen.findByText('prop-004')
    expect(screen.queryByText('Dar de baja')).toBeNull()
    expect(within(screen.getByRole('main')).getByText('Nueva propiedad')).toBeTruthy()
    expect(within(screen.getByLabelText('Menú del panel')).getByText('Vendedor')).toBeTruthy()
  })
  it('el vendedor que escribe /sitio/inicio ve un aviso de permisos, no el editor, y no se piden datos del sitio', async () => {
    conRol('VENDEDOR'); abrir('/sitio/inicio')
    expect(await screen.findByText('No tiene permisos para gestionar el sitio web.')).toBeTruthy()
    expect(screen.queryByText('Guardar y publicar la portada')).toBeNull()
    expect(ultimo('GET', '/api/admin/sitio')).toBeUndefined()
  })
  it('el gerente sí abre el editor del sitio', async () => {
    conRol('GERENTE'); abrir('/sitio/contacto')
    expect(await screen.findByText('Guardar y publicar')).toBeTruthy()
    expect(screen.queryByText('No tiene permisos para gestionar el sitio web.')).toBeNull()
  })
  it('el gerente puede dar de baja', async () => {
    conRol('GERENTE'); abrir('/propiedades'); await screen.findByText('prop-004')
    expect(screen.getAllByText('Dar de baja').length).toBeGreaterThan(0)
  })
  it('un usuario sin el permiso de ver propiedades no ve esa sección en el menú', async () => {
    conRol('ADMIN'); (await import('./backendFalso')).usuario.permisos = ['CUENTA_PROPIA']
    abrir('/cuenta'); await screen.findByRole('heading', { name: 'Mi cuenta' })
    const menu = screen.getByRole('navigation')
    expect(within(menu).queryByText('Propiedades')).toBeNull(); expect(within(menu).queryByText('Nueva propiedad')).toBeNull()
  })
})

describe('Insignia del login', () => {
  it('la regla del subtítulo no alcanza a la insignia (antes la descentraba y la ponía gris)', () => {
    const css = readFileSync('src/ui.module.css', 'utf-8')
    expect(css).not.toMatch(/\.loginMarca span\s*\{/)
    expect(css).toMatch(/\.loginMarca div span\s*\{/)
    expect(css).toMatch(/\.marcaInsignia\s*\{[^}]*justify-content:\s*center/)
  })
})

// ---------------- Etapa 2B: usuarios, Mi cuenta, contraseña temporal y responsable ----------------
describe('Usuarios', () => {
  it('el administrador ve el listado y puede crear usuarios', async () => {
    abrir('/usuarios'); expect(await screen.findByText('vera@sn.local')).toBeTruthy()
    expect(screen.getByText('Desactivado')).toBeTruthy()
    expect(within(screen.getByRole('main')).getByText('Nuevo usuario')).toBeTruthy()
  })
  it('el alta muestra la contraseña temporal una sola vez en la pantalla del usuario', async () => {
    abrir('/usuarios/nuevo'); await screen.findByRole('heading', { name: 'Nuevo usuario' })
    fireEvent.change(campo('Nombre'), { target: { value: 'Nora' } }); fireEvent.change(campo('Apellido'), { target: { value: 'Ruiz' } })
    fireEvent.change(campo('Email'), { target: { value: 'nora@sn.local' } }); fireEvent.change(campo('Rol'), { target: { value: 'GERENTE' } })
    fireEvent.click(screen.getByText('Crear usuario'))
    expect(await screen.findByText('Tmp7kQ9xWz2a')).toBeTruthy()
    expect((ultimo('POST', '/api/admin/usuarios')!.cuerpo as any).rol).toBe('GERENTE')
    usuariosDb.lista = usuariosDb.lista.filter((u) => u.id !== 60)
  })
  it('un email repetido muestra el mensaje del backend', async () => {
    abrir('/usuarios/nuevo'); await screen.findByRole('heading', { name: 'Nuevo usuario' })
    fireEvent.change(campo('Nombre'), { target: { value: 'X' } }); fireEvent.change(campo('Apellido'), { target: { value: 'Y' } })
    fireEvent.change(campo('Email'), { target: { value: 'VERA@sn.local' } }); fireEvent.click(screen.getByText('Crear usuario'))
    expect(await screen.findByText('Ya existe otro usuario con ese email')).toBeTruthy()
  })
  it('editar, desactivar y generar contraseña temporal de otro usuario', async () => {
    abrir('/usuarios/50'); await waitFor(() => expect(campo('Nombre').value).toBe('Vera'))
    fireEvent.change(campo('Rol'), { target: { value: 'GERENTE' } }); fireEvent.click(screen.getByText('Guardar cambios'))
    expect(await screen.findByText('Cambios guardados.')).toBeTruthy()
    fireEvent.click(screen.getByText('Desactivar cuenta')); expect(await screen.findByText('Cuenta desactivada.')).toBeTruthy()
    expect((ultimo('PATCH', '/api/admin/usuarios/50/estado')!.cuerpo as any)).toEqual({ activo: false })
    fireEvent.click(screen.getByText('Generar contraseña temporal')); expect(await screen.findByText('Nueva8mPq3Rt')).toBeTruthy()
    Object.assign(usuariosDb.lista[1], { rol: 'VENDEDOR', activo: true, debeCambiarPassword: false })
  })
  it('en su propia ficha no puede cambiar su rol ni desactivarse', async () => {
    abrir('/usuarios/1'); await waitFor(() => expect(campo('Nombre').value).toBe('Paula'))
    expect((campo('Rol') as unknown as HTMLSelectElement).disabled).toBe(true)
    expect(screen.queryByText('Desactivar cuenta')).toBeNull()
  })
  it('el gerente solo consulta: sin "Nuevo usuario" ni "Editar"', async () => {
    conRol('GERENTE'); abrir('/usuarios'); await screen.findByText('vera@sn.local')
    expect(screen.queryByText('Nuevo usuario')).toBeNull(); expect(screen.queryByText('Editar')).toBeNull()
  })
  it('el vendedor no tiene Usuarios en el menú', async () => {
    conRol('VENDEDOR'); abrir('/cuenta'); await screen.findByRole('heading', { name: 'Mi cuenta' })
    expect(within(screen.getByRole('navigation')).queryByText('Usuarios')).toBeNull()
  })
})

describe('Mi cuenta y contraseña', () => {
  it('edita un dato en su fila y cambia el email confirmando la contraseña', async () => {
    abrir('/cuenta'); await screen.findByText('Información de la cuenta')
    fireEvent.click(screen.getByLabelText('Editar apellido'))
    fireEvent.change(campo('Apellido'), { target: { value: 'Gómez' } }); fireEvent.click(screen.getByText('Guardar'))
    expect(await screen.findByText('Cambios guardados.')).toBeTruthy()
    expect((ultimo('PUT', '/api/admin/cuenta')!.cuerpo as any)).toMatchObject({ nombre: 'Paula', apellido: 'Gómez' })
    fireEvent.click(screen.getByLabelText('Editar email'))
    fireEvent.change(campo('Email'), { target: { value: 'nuevo@sn.local' } }); fireEvent.click(screen.getByText('Guardar'))
    expect(await screen.findByText('La contraseña actual no es correcta')).toBeTruthy()
    fireEvent.change(campo(/Contraseña actual/), { target: { value: 'Clave-Correcta' } }); fireEvent.click(screen.getByText('Guardar'))
    expect(await screen.findByText(/Email actualizado/)).toBeTruthy()
    Object.assign(usuariosDb.lista[0], { apellido: 'Prueba', email: 'paula@sn.local' })
  })
  it('tema: claro, oscuro o según el sistema, aplicado de inmediato y recordado', async () => {
    abrir('/cuenta'); await screen.findByText('Preferencias')
    fireEvent.click(screen.getByRole('radio', { name: /Oscuro/ }))
    expect(document.documentElement.dataset.tema).toBe('oscuro'); expect(localStorage.getItem('sn-panel-tema')).toBe('oscuro')
    expect(screen.getByRole('radio', { name: /Oscuro/ }).getAttribute('aria-checked')).toBe('true')
    fireEvent.click(screen.getByRole('radio', { name: /Claro/ })); expect(document.documentElement.dataset.tema).toBe('claro')
    fireEvent.click(screen.getByRole('radio', { name: /Según el sistema/ }))
    expect(document.documentElement.dataset.tema).toBeUndefined(); expect(localStorage.getItem('sn-panel-tema')).toBeNull()
  })
  it('cambiar contraseña: las dos nuevas deben coincidir; al guardar vuelve a Mi cuenta', async () => {
    abrir('/cuenta/contrasena'); await screen.findByRole('heading', { name: 'Cambiar contraseña' })
    fireEvent.change(campo('Contraseña actual'), { target: { value: 'Clave-Correcta' } })
    fireEvent.change(campo('Nueva contraseña'), { target: { value: 'OtraClave2026' } }); fireEvent.change(campo('Repetir la nueva contraseña'), { target: { value: 'distinta' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await screen.findByText('Las contraseñas no coinciden')).toBeTruthy(); expect(ultimo('PUT', '/cuenta/contrasena')).toBeUndefined()
    fireEvent.change(campo('Repetir la nueva contraseña'), { target: { value: 'OtraClave2026' } })
    fireEvent.click(screen.getByRole('button', { name: 'Cambiar contraseña' }))
    expect(await screen.findByText('Contraseña actualizada.')).toBeTruthy()
  })
  it('con contraseña temporal, cualquier pantalla lleva al cambio obligatorio', async () => {
    falso.usuario.debeCambiarPassword = true
    abrir('/propiedades'); expect(await screen.findByText(/Su cuenta tiene una contraseña temporal/)).toBeTruthy()
    expect(screen.queryByText('prop-004')).toBeNull()
  })
})

describe('Responsable de las propiedades', () => {
  it('el vendedor ve "Editar" en las suyas y "Ver" en las ajenas', async () => {
    conRol('VENDEDOR', 50); abrir('/propiedades'); await screen.findByText('prop-004')
    const filas = screen.getAllByRole('row')
    expect(within(filas.find((f) => f.textContent!.includes('prop-004'))!).getByText('Editar')).toBeTruthy()
    expect(within(filas.find((f) => f.textContent!.includes('terr-001'))!).getByText('Ver')).toBeTruthy()
    expect(screen.getByText('Sin asignar')).toBeTruthy()
  })
  it('una propiedad ajena se abre en solo lectura, sin guardar ni acciones de fotos', async () => {
    conRol('VENDEDOR', 99); abrir('/propiedades/4')
    expect(await screen.findByText(/asignada a otro responsable/)).toBeTruthy()
    expect(screen.queryByText('Guardar cambios')).toBeNull(); expect(screen.queryByText('Subir foto')).toBeNull()
    expect((campo('Título') as HTMLInputElement).closest('fieldset')!.disabled).toBe(true)
  })
  it('el administrador elige el responsable y se envía al guardar', async () => {
    abrir('/propiedades/4'); await waitFor(() => expect(campo('Título').value).toBe('Departamento en San Jose'))
    const selector = await waitFor(() => { const s = campo('Usuario responsable') as unknown as HTMLSelectElement; expect(s.options.length).toBeGreaterThan(1); return s })
    expect([...selector.options].map((o) => o.textContent)).not.toContain('Gino Prueba') // desactivado y no asignado
    fireEvent.change(selector, { target: { value: '1' } }); fireEvent.click(screen.getByText('Guardar cambios'))
    await screen.findByText('Cambios guardados.')
    expect((ultimo('PUT', '/api/admin/propiedades/4')!.cuerpo as any).idResponsable).toBe(1)
  })
})

// ---------------- Etapa 3: consultas del chat IA ----------------
describe('Consultas del chat IA', () => {
  it('historial sin textos de ayuda redundantes y las antiguas como "No registrado"', async () => {
    abrir('/consultas'); expect(await screen.findByText('¿Tiene cochera?')).toBeTruthy()
    expect(screen.queryByText(/No significa que el cliente esté esperando una respuesta/)).toBeNull()
    const filas = screen.getAllByRole('row')
    expect(within(filas.find((f) => f.textContent!.includes('cochera'))!).getByText('No registrado')).toBeTruthy()
    expect(within(filas.find((f) => f.textContent!.includes('alquiler'))!).getByText('General')).toBeTruthy()
    expect(screen.queryByText(/esperan respuesta|urgente|requiere atención/i)).toBeNull() // las pendientes no son tareas
  })
  it('los filtros se envían al backend', async () => {
    abrir('/consultas'); await screen.findByText('¿Tiene cochera?')
    fireEvent.change(screen.getByLabelText('Buscar'), { target: { value: 'cochera' } })
    fireEvent.change(screen.getByLabelText('Resultado'), { target: { value: 'NO_REGISTRADO' } })
    fireEvent.click(screen.getByText('Últimos 30 días'))
    await waitFor(() => {
      const u = ultimo('GET', '/api/admin/consultas?')!.url
      expect(u).toContain('texto=cochera'); expect(u).toContain('resultado=NO_REGISTRADO'); expect(u).toMatch(/desde=\d{4}-\d{2}-\d{2}/)
    })
  })
  it('detalle de una consulta antigua: sin resultado ni recomendadas inventadas, con su conversación', async () => {
    abrir('/consultas/2'); expect(await screen.findByText('No dispongo de esa información.')).toBeTruthy()
    expect(screen.getAllByText(/No registrado \(consulta anterior al registro/).length).toBe(2)
    expect(screen.getByText('¿Cuánto cuesta?')).toBeTruthy()
    expect(screen.getByText(/esta consulta/)).toBeTruthy()
  })
  it('marcar como revisada registra quién lo hizo', async () => {
    abrir('/consultas/2'); fireEvent.click(await screen.findByText('Marcar como revisada'))
    expect(await screen.findByText(/Revisada por Paula Admin/)).toBeTruthy()
    expect((ultimo('PATCH', '/api/admin/consultas/2/revision')!.cuerpo as any)).toEqual({ estado: 'REVISADA' })
    fireEvent.click(screen.getByText('Marcar como sin revisar')); expect(await screen.findByText('Sin revisar')).toBeTruthy()
  })
  it('detalle de una consulta nueva muestra las recomendadas con enlace', async () => {
    abrir('/consultas/9'); await screen.findByText('Casas en alquiler')
    expect(screen.getByText('prop-004 · Departamento en San Jose').closest('a')!.getAttribute('href')).toBe('/propiedades/4')
  })
  it('pestañas: ADMIN y GERENTE ven Historial, Análisis técnico y Clientes potenciales (Calidad se retiró)', async () => {
    abrir('/consultas'); await screen.findByText('¿Tiene cochera?')
    const pestanas = screen.getByRole('navigation', { name: 'Secciones de Consultas IA' })
    expect(within(pestanas).getAllByRole('link').map((l) => l.textContent)).toEqual(['Historial', 'Análisis técnico', 'Clientes potenciales'])
    expect(screen.queryByText('Calidad de respuestas')).toBeNull()
  })
  it('historial paginado: pide la página al backend y muestra el total', async () => {
    falso.consultasDb.total = 45
    abrir('/consultas'); expect(await screen.findByText(/Mostrando 1–2 de 45 consultas/)).toBeTruthy()
    expect(screen.getByText('Página 1 de 3')).toBeTruthy()
    fireEvent.click(screen.getByLabelText('Página siguiente'))
    await waitFor(() => expect(ultimo('GET', '/api/admin/consultas?')!.url).toContain('pagina=2'))
    falso.consultasDb.total = 2
  })
  it('análisis técnico: solo los 7 indicadores aprobados, en gráficos circulares con porcentajes', async () => {
    abrir('/consultas/analisis'); expect(await screen.findByText('Preguntas recibidas por el asistente')).toBeTruthy()
    expect(screen.getByText('Conversaciones')).toBeTruthy()
    const fichas = ['Sin servicio', 'Propiedades más consultadas', 'Zonas más consultadas', 'Rangos de precios', 'Temas consultados']
    for (const t of fichas) expect(screen.getByRole('region', { name: t })).toBeTruthy()
    // Cada ficha tiene su gráfico (imagen con descripción) y la leyenda con cantidad y porcentaje
    expect(screen.getAllByRole('img').length).toBe(5)
    const rangos = screen.getByRole('region', { name: 'Rangos de precios' })
    expect(within(rangos).getByText('Alquiler: Gs. 2.000.000 a Gs. 4.000.000')).toBeTruthy()
    expect(within(rangos).getByText('100 %')).toBeTruthy()
    expect(within(screen.getByRole('region', { name: 'Sin servicio' })).getByRole('img').getAttribute('aria-label')).toBe('Sin servicio: 0 de 2 (0 %)')
    expect(within(screen.getByRole('region', { name: 'Temas consultados' })).getByText('Inferido')).toBeTruthy()
    // Lo que se quitó
    for (const t of [/Iniciadas desde una propiedad/, /Características más solicitadas/, /Tipos de propiedad/, /Preguntas repetidas/, /registro completo/i])
      expect(screen.queryByText(t)).toBeNull()
  })
  it('la dirección de Calidad de respuestas lleva al historial', async () => {
    abrir('/consultas/calidad'); expect(await screen.findByText('¿Tiene cochera?')).toBeTruthy()
    expect(screen.queryByText('Consultas con problemas de respuesta')).toBeNull()
  })
  it('sin consultas en el periodo muestra un aviso en lugar de fichas vacías', async () => {
    const original = falso.consultasDb.analisis.indicadores.consultas
    falso.consultasDb.analisis.indicadores.consultas = 0
    abrir('/consultas/analisis'); expect(await screen.findByText('No hay consultas en este periodo.')).toBeTruthy()
    falso.consultasDb.analisis.indicadores.consultas = original
  })
  it('el vendedor ve el historial (sin la aclaración de alcance) pero no el análisis', async () => {
    conRol('VENDEDOR', 50); abrir('/consultas'); await screen.findByText('¿Tiene cochera?')
    const pestanas = screen.getByRole('navigation', { name: 'Secciones de Consultas IA' })
    expect(within(pestanas).getAllByRole('link').map((l) => l.textContent)).toEqual(['Historial', 'Clientes potenciales'])
    expect(screen.queryByText(/consultas generales y las relacionadas con sus propiedades/)).toBeNull()
  })
})

// ---------------- Etapa 3, entrega 2: clientes potenciales ----------------
describe('Clientes potenciales', () => {
  it('listado con fichas, estados y responsable', async () => {
    abrir('/consultas/clientes'); expect(await screen.findByText('Ana Torres')).toBeTruthy()
    expect(screen.getByText('Pendientes de contacto')).toBeTruthy()
    const filas = screen.getAllByRole('row')
    const ana = filas.find((f) => f.textContent!.includes('Ana Torres'))!
    expect(within(ana).getByText('Pendiente de contacto')).toBeTruthy(); expect(within(ana).getByText('Sin asignar')).toBeTruthy()
    expect(within(filas.find((f) => f.textContent!.includes('Carlos López'))!).getByText('Formulario de contacto')).toBeTruthy()
    expect([...(screen.getByLabelText('Origen') as HTMLSelectElement).options].map((o) => o.textContent)).toContain('Formulario de contacto')
    fireEvent.change(screen.getByLabelText('Estado de seguimiento'), { target: { value: 'CONTACTADO' } })
    await waitFor(() => expect(ultimo('GET', '/api/admin/clientes-potenciales?')!.url).toContain('estado=CONTACTADO'))
  })
  it('detalle: cambiar estado y asignar responsable (ADMIN), con la aceptación registrada', async () => {
    abrir('/consultas/clientes/20'); await screen.findByText('Datos de contacto')
    expect(screen.getByText(/Aceptó el .* \(texto versión 2026-10\)/)).toBeTruthy()
    fireEvent.change(screen.getByLabelText('Cambiar estado'), { target: { value: 'CONTACTADO' } })
    expect(await screen.findByText('Estado actualizado.')).toBeTruthy()
    expect((ultimo('PATCH', '/clientes-potenciales/20/estado')!.cuerpo as any)).toEqual({ estado: 'CONTACTADO' })
    await waitFor(() => expect((screen.getByLabelText('Responsable') as HTMLSelectElement).options.length).toBeGreaterThan(1))
    fireEvent.change(screen.getByLabelText('Responsable'), { target: { value: '50' } })
    expect(await screen.findByText('Responsable actualizado.')).toBeTruthy()
    expect((ultimo('PATCH', '/clientes-potenciales/20/responsable')!.cuerpo as any)).toEqual({ idResponsable: 50 })
    falso.clientesDb.detalle.estado = 'PENDIENTE'; falso.clientesDb.detalle.responsable = null
  })
  it('el vendedor actualiza el estado pero no asigna responsables', async () => {
    conRol('VENDEDOR', 50); abrir('/consultas/clientes/20'); await screen.findByText('Datos de contacto')
    expect(screen.getByLabelText('Cambiar estado')).toBeTruthy()
    expect(screen.queryByLabelText('Responsable')).toBeNull()
  })
  it('alta manual: medio de contacto y error si falta teléfono y email', async () => {
    conRol('VENDEDOR', 50); abrir('/consultas/clientes/nuevo'); await screen.findByText('Datos del interesado')
    expect(screen.getByText('Usted quedará como responsable del seguimiento.')).toBeTruthy()
    fireEvent.change(campo('Nombre'), { target: { value: 'Luis' } }); fireEvent.change(campo('Medio de contacto'), { target: { value: 'LLAMADA' } })
    fireEvent.click(screen.getByText('Registrar'))
    expect(await screen.findByText('Indique un teléfono o un email para que podamos contactarle')).toBeTruthy()
    fireEvent.change(campo('Teléfono'), { target: { value: '0991 222 333' } }); fireEvent.click(screen.getByText('Registrar'))
    expect(await screen.findByText('Cliente potencial registrado.')).toBeTruthy()
    const cuerpo = ultimo('POST', '/api/admin/clientes-potenciales')!.cuerpo as any
    expect(cuerpo.origen).toBe('LLAMADA'); expect(cuerpo.idResponsable).toBeNull()
  })
})

describe('Tablas etiqueta / valor', () => {
  it('la columna de etiquetas es más ancha y hay separación entre columnas', () => {
    const css = readFileSync('src/ui.module.css', 'utf-8')
    // 220 px fijos: la misma columna de etiquetas que los formularios en filas
    expect(css).toMatch(/\.datos \{[^}]*220px minmax\(0, 1fr\)[^}]*column-gap: 24px/)
  })
})

// ---------------- Formularios en filas horizontales ----------------
describe('Formularios en filas horizontales', () => {
  it('cada campo es una fila: etiqueta a la izquierda (220 px) y control a la derecha; en celular, la etiqueta arriba', () => {
    const css = readFileSync('src/ui.module.css', 'utf-8')
    expect(css).toMatch(/\.grilla \{ display: flex; flex-direction: column; \}/)
    expect(css).toMatch(/\.grilla > \.campo \{[^}]*grid-template-columns: 220px minmax\(0, 1fr\)/)
    expect(css).toMatch(/\.grilla > \.campo > \.etiqueta \{ grid-column: 1;/)
    expect(css).toMatch(/@media \(max-width: 640px\) \{\s*\.grilla > \.campo \{ grid-template-columns: minmax\(0, 1fr\); \}/)
    expect(css).toMatch(/\.tarjeta h2 \{[^}]*font-size: 19px;[^}]*font-weight: 700;[^}]*color: var\(--verde-texto\)/)
  })
  it('propiedad: las casillas tienen su fila ("Ambientes") y el responsable va en una fila', async () => {
    abrir('/propiedades/4'); await waitFor(() => expect(campo('Título').value).toBe('Departamento en San Jose'))
    const ambientes = screen.getByText('Ambientes').parentElement!
    expect(within(ambientes).getByLabelText('Sala')).toBeTruthy(); expect(within(ambientes).getByLabelText('Cocina')).toBeTruthy()
    const responsable = await screen.findByText('Usuario responsable')
    expect(responsable.parentElement!.parentElement!.className).toContain('grilla')
    expect(campo('Título').closest('[class*="grilla"]')).toBeTruthy()
  })
  it('usuario nuevo y sitio: los campos van en filas, sin envoltorios extra', async () => {
    abrir('/usuarios/nuevo')
    const nombre = await screen.findByText('Nombre')
    expect(nombre.parentElement!.parentElement!.className).toContain('grilla')
    cleanup(); abrir('/sitio/inicio')
    const titulo = await screen.findByText('Título de la portada')
    expect(titulo.parentElement!.parentElement!.className).toContain('grilla')   // ya no hay un div.campo intermedio
  })
})

// ---------------- Gestión del sitio web (entrega A) ----------------
describe('Gestión del sitio web', () => {
  it('es un único elemento del menú, con pestañas internas; el vendedor no lo ve', async () => {
    abrir('/sitio'); expect(await screen.findByRole('heading', { name: 'Gestión del sitio web' })).toBeTruthy()
    expect(within(screen.getByRole('navigation', { name: 'Secciones del sitio web' })).getAllByRole('link').map((l) => l.textContent)).toEqual(['Inicio', 'Nosotros', 'Contacto'])   // Servicios pasó a Nosotros
    expect(within(screen.getByLabelText('Menú del panel')).getAllByText('Gestión del sitio web')).toHaveLength(1)
    cleanup(); conRol('VENDEDOR', 50); abrir('/cuenta'); await screen.findByRole('heading', { name: 'Mi cuenta' })
    expect(within(screen.getByLabelText('Menú del panel')).queryByText('Gestión del sitio web')).toBeNull()
  })
  it('Inicio: editar el título, subir la imagen de portada y publicar', async () => {
    abrir('/sitio/inicio'); await waitFor(() => expect(campo('Título de la portada').value).toBe('Encuentra el lugar'))
    expect(screen.queryByText(/Sobre nosotros/)).toBeNull()                       // el bloque se retiró
    expect(screen.queryByText(/Texto pequeño sobre el título/)).toBeNull()       // y el texto sobre el título
    fireEvent.change(campo('Título de la portada'), { target: { value: 'Tu próximo hogar' } })
    fireEvent.change(screen.getByLabelText('Archivo para Imagen de fondo'), { target: { files: [new File(['x'], 'p.png', { type: 'image/png' })] } })
    await waitFor(() => expect(ultimo('POST', '/api/admin/sitio/imagenes')).toBeTruthy())
    fireEvent.click(screen.getByText('Guardar y publicar la portada'))
    expect(await screen.findByText('Portada publicada.')).toBeTruthy()
    const cuerpo = ultimo('PUT', '/sitio/secciones/inicio')!.cuerpo as any
    expect(Object.keys(cuerpo).sort()).toEqual(['imagenPortada', 'subtitulo', 'titulo']) // sin los campos retirados
    expect(cuerpo.titulo).toBe('Tu próximo hogar'); expect(cuerpo.imagenPortada).toMatch(/^\/uploads\/sitio-001\//)
  })
  it('Inicio: avisa formatos no admitidos sin subir el archivo', async () => {
    abrir('/sitio/inicio'); await waitFor(() => expect(campo('Título de la portada').value).toBeTruthy())
    fireEvent.change(screen.getByLabelText('Archivo para Imagen de fondo'), { target: { files: [new File(['x'], 'a.pdf', { type: 'application/pdf' })] } })
    expect(await screen.findByText('Formato no admitido. Use imágenes JPEG, PNG o WebP.')).toBeTruthy()
    expect(ultimo('POST', '/api/admin/sitio/imagenes')).toBeUndefined()
  })
  it('Contacto: aviso de datos provisionales, error por campo y publicación', async () => {
    abrir('/sitio/contacto'); expect(await screen.findByText(/provisionales/)).toBeTruthy()
    for (const t of ['Empresa', 'Contacto', 'Horario de atención']) expect(screen.getAllByText(t).length).toBeGreaterThan(0)
    expect(screen.queryByText(/Facebook|Instagram|Tipo de cambio|Mensajes/i)).toBeNull() // alcance acordado
    fireEvent.change(campo('WhatsApp'), { target: { value: '0981-12' } }); fireEvent.click(screen.getByText('Guardar y publicar'))
    expect(await screen.findByText(/formato internacional, solo números/)).toBeTruthy()
    fireEvent.change(campo('Email'), { target: { value: 'info@sn.com.py' } }); fireEvent.change(campo('Teléfono'), { target: { value: '061 500 000' } })
    fireEvent.change(campo('WhatsApp'), { target: { value: '+595 981 123 456' } }); fireEvent.click(screen.getByText('Guardar y publicar'))
    expect(await screen.findByText('Datos de contacto publicados.')).toBeTruthy()
    expect(screen.queryByText(/provisionales/)).toBeNull()
  })
  it('Ofertas (dentro de Inicio): sin selección avisa del comportamiento al azar; agrega solo disponibles y publica', async () => {
    db.lista = db.lista.map((x) => (x.id === 20 ? { ...x, estado: 'Vendido' } : x))
    abrir('/sitio/ofertas')                                                        // la ruta anterior lleva a Inicio
    expect(await screen.findByText('Ofertas destacadas')).toBeTruthy()
    expect(await screen.findByText(/seis propiedades disponibles elegidas al azar/)).toBeTruthy()
    const selector = screen.getByLabelText('Propiedad a destacar') as HTMLSelectElement
    await waitFor(() => expect(selector.options.length).toBe(2))                // solo la disponible (la vendida no aparece)
    fireEvent.change(selector, { target: { value: '4' } }); fireEvent.click(screen.getByText('Agregar'))
    expect(screen.getByText('Hay cambios sin guardar.')).toBeTruthy()
    fireEvent.click(within(screen.getByText('Ofertas destacadas').closest('section')!).getByText('Guardar y publicar'))
    expect(await screen.findByText('Ofertas publicadas.')).toBeTruthy()
    expect((ultimo('PUT', '/api/admin/sitio/ofertas')!.cuerpo as any)).toEqual({ ids: [4] })
    falso.sitioDb.ofertas = []
  })
  it('Servicios (en la pestaña Nosotros): agregar un servicio, error por elemento y publicación', async () => {
    abrir('/sitio/servicios')                                                   // la dirección vieja lleva a Nosotros
    await waitFor(() => expect(campo('Servicio 1: título').value).toBe('Compra y Venta'))
    expect(screen.getByRole('link', { name: 'Nosotros' }).className).toContain('pestanaActiva')
    fireEvent.click(screen.getByText('Agregar servicio'))
    fireEvent.change(campo('Servicio 3: título'), { target: { value: 'Tasaciones' } })
    fireEvent.click(screen.getByText('Guardar y publicar'))
    expect(await screen.findByText('Es obligatorio')).toBeTruthy()             // el texto del servicio 3 falta
    expect(screen.getByRole('alert').textContent).toMatch(/^Servicios: .*Sí se publicaron la historia y los valores\.$/)  // dice qué se publicó
    fireEvent.change(campo('Servicio 3: texto'), { target: { value: 'Valuamos su propiedad.' } })
    fireEvent.click(screen.getByText('Guardar y publicar'))
    expect(await screen.findByText('Página Nosotros publicada (historia, valores y servicios).')).toBeTruthy()
    const cuerpo = ultimo('PUT', '/sitio/secciones/servicios')!.cuerpo as any
    expect(cuerpo.servicios).toHaveLength(3)
    expect([cuerpo.bloqueTitulo, cuerpo.bloqueTexto]).toEqual(['', ''])         // el bloque inferior se retiró
  })
  it('Nosotros: cifras y valores se publican en su orden; sin "Por qué elegirnos" (se guarda vacío)', async () => {
    abrir('/sitio/nosotros'); await waitFor(() => expect(campo('Cifra 1: valor').value).toBe('+150'))
    expect(screen.queryByText(/Por qué elegirnos/)).toBeNull(); expect(screen.queryByText(/Razón 1/)).toBeNull()
    expect(screen.queryByText(/Bloque inferior|Título del bloque/)).toBeNull()
    expect(screen.getAllByText('Subir').length).toBeGreaterThan(1)               // valores y servicios
    fireEvent.click(screen.getByText('Agregar valor'))
    fireEvent.change(campo('Valor 2: título'), { target: { value: 'Cercanía' } }); fireEvent.change(campo('Valor 2: texto'), { target: { value: 'Atención personal.' } })
    fireEvent.click(screen.getAllByText('Subir')[1])                            // el valor nuevo pasa al primer lugar
    fireEvent.click(screen.getByText('Guardar y publicar'))
    expect(await screen.findByText('Página Nosotros publicada (historia, valores y servicios).')).toBeTruthy()
    const cuerpo = ultimo('PUT', '/sitio/secciones/nosotros')!.cuerpo as any
    expect(cuerpo.valores.map((v: any) => v.titulo)).toEqual(['Cercanía', 'Confianza'])
    expect(cuerpo.razones).toEqual([])
    expect(ultimo('PUT', '/sitio/secciones/servicios')).toBeTruthy()            // un solo botón guarda las dos partes
  })
})

describe('Botón Volver', () => {
  it('vuelve a la pestaña anterior, no siempre al historial', async () => {
    abrir('/consultas'); await screen.findByText('¿Tiene cochera?')
    const pestanas = () => screen.getByRole('navigation', { name: 'Secciones de Consultas IA' })
    fireEvent.click(within(pestanas()).getByText('Análisis técnico')); await screen.findByText('Preguntas recibidas por el asistente')
    fireEvent.click(within(pestanas()).getByText('Clientes potenciales')); await screen.findByText('Ana Torres')
    fireEvent.click(screen.getByLabelText('Volver a la pantalla anterior'))
    expect(await screen.findByText('Preguntas recibidas por el asistente')).toBeTruthy() // la anterior: Análisis técnico
  })
  it('desde un detalle vuelve a la lista de la que vino', async () => {
    abrir('/consultas'); await screen.findByText('¿Tiene cochera?')
    fireEvent.click(screen.getAllByText('Ver')[0]); await screen.findByText('Respuesta del asistente')
    fireEvent.click(screen.getByLabelText('Volver a la pantalla anterior'))
    expect(await screen.findByText('¿Tiene cochera?')).toBeTruthy()
  })
  it('si la página se abrió directamente, lleva a su sección sin salir del panel', async () => {
    abrir('/usuarios/50'); await waitFor(() => expect(campo('Nombre').value).toBe('Vera'))
    fireEvent.click(screen.getByLabelText('Volver a la pantalla anterior'))
    expect(await screen.findByText('vera@sn.local')).toBeTruthy()             // la lista de usuarios
  })
})

describe('Solo guaraníes (W4)', () => {
  it('el formulario no ofrece elegir moneda: el precio se carga en Gs. y se envía como PYG', async () => {
    abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda')
    expect(screen.queryByLabelText('Moneda 1')).toBeNull()
    expect(screen.getByText('Precio (Gs.)')).toBeTruthy()
    fireEvent.click(screen.getByText('+ Agregar Alquiler'))
    expect(screen.getByText('Precio mensual (Gs.)')).toBeTruthy(); expect(screen.queryByLabelText('Moneda 2')).toBeNull()
  })
  it('el listado muestra los precios en guaraníes con puntos de miles', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    expect(screen.getByText('Alquiler Gs. 4.000.000 / mes')).toBeTruthy()
    expect(screen.getByText('Venta Gs. 300.000.000')).toBeTruthy()
    expect(screen.queryByText(/USD/)).toBeNull()
  })
  it('al editar, una propiedad existente conserva su precio y se guarda como PYG', async () => {
    abrir('/propiedades/4'); await screen.findByText('Editar prop-004')
    expect((screen.getByLabelText('Precio 1') as HTMLInputElement).value).toBe('4000000')
    fireEvent.click(screen.getByText('Guardar cambios'))
    await waitFor(() => expect(ultimo('PUT', '/api/admin/propiedades/4')).toBeTruthy())
    expect((ultimo('PUT', '/api/admin/propiedades/4')!.cuerpo as any).operaciones).toEqual([{ operacion: 'Alquiler', precio: 4000000, moneda: 'PYG' }])
  })
})

// ---------------- Inicio del panel ----------------
describe('Inicio del panel', () => {
  const fichas = () => within(screen.getByRole('group', { name: 'Accesos rápidos' })).getAllByRole('link')
  const titulos = () => fichas().map((f) => f.querySelector('strong')!.textContent)
  it('entrar a /admin/ abre Inicio con el saludo y el nombre', async () => {
    abrir('/')
    expect(await screen.findByRole('heading', { name: /^Buen(os|as) (días|tardes|noches), Paula$/ })).toBeTruthy()
    expect(screen.getByText(/¿Por dónde empezamos\?|Este es su panel de trabajo\.|¿Qué vamos a hacer hoy\?|Todo listo para seguir\./)).toBeTruthy()
  })
  it('saludo según la hora', () => {
    expect([5, 11, 12, 18, 19, 23, 0, 4].map(saludoDeLaHora)).toEqual(
      ['Buenos días', 'Buenos días', 'Buenas tardes', 'Buenas tardes', 'Buenas noches', 'Buenas noches', 'Buenas noches', 'Buenas noches'])
  })
  it('administrador: clientes por contactar, nueva propiedad, nuevo usuario y sitio web', async () => {
    abrir('/'); await screen.findByText('Cliente por contactar')
    expect(titulos()).toEqual(['Cliente por contactar', 'Nueva propiedad', 'Nuevo usuario', 'Gestión del sitio web'])
    const clientes = fichas()[0]
    expect(clientes.textContent).toContain('1')
    expect(clientes.className).toContain('fichaInicioAviso')                    // destacada: hay que atenderlo
    expect(clientes.getAttribute('href')).toBe('/consultas/clientes?estado=PENDIENTE')
  })
  it('gerente: clientes, nueva propiedad, sitio web y consultas; vendedor: clientes, nueva propiedad, consultas y listado', async () => {
    conRol('GERENTE'); abrir('/'); await screen.findByText('Cliente por contactar')
    expect(titulos()).toEqual(['Cliente por contactar', 'Nueva propiedad', 'Gestión del sitio web', 'Consultas IA'])
    cleanup(); conRol('VENDEDOR', 50); abrir('/'); await screen.findByText('Cliente por contactar')
    expect(titulos()).toEqual(['Cliente por contactar', 'Nueva propiedad', 'Consultas IA', 'Listado de propiedades'])
    expect(fichas().length).toBeGreaterThanOrEqual(2); expect(fichas().length).toBeLessThanOrEqual(4)
  })
  it('la ficha de clientes abre la lista ya filtrada en "Pendientes"', async () => {
    abrir('/'); fireEvent.click(await screen.findByText('Cliente por contactar'))
    await waitFor(() => expect(ultimo('GET', '/api/admin/clientes-potenciales?')!.url).toContain('estado=PENDIENTE'))
    expect((screen.getByLabelText('Estado de seguimiento') as HTMLSelectElement).value).toBe('PENDIENTE')
  })
  it('sin pendientes: "Al día", sin destacar; si falla la consulta, lo dice (no inventa un número)', async () => {
    vi.stubGlobal('fetch', vi.fn(async (u: RequestInfo | URL, init?: RequestInit) => {
      if (String(u).includes('/api/admin/clientes-potenciales?')) return new Response(JSON.stringify({ elementos: [], total: 0, pagina: 1, tamano: 20,
        conteos: { total: 0, pendientes: 0, contactados: 0, descartados: 0 } }), { status: 200, headers: { 'Content-Type': 'application/json' } })
      return fetchFalso(u, init)
    }))
    abrir('/'); expect(await screen.findByText('Al día: no hay clientes esperando contacto.')).toBeTruthy()
    expect(fichas()[0].className).not.toContain('fichaInicioAviso')
    cleanup()
    vi.stubGlobal('fetch', vi.fn(async (u: RequestInfo | URL, init?: RequestInit) =>
      String(u).includes('/api/admin/clientes-potenciales?') ? new Response('{}', { status: 500 }) : fetchFalso(u, init)))
    abrir('/'); expect(await screen.findByText('No se pudo consultar cuántos esperan contacto.')).toBeTruthy()
  })
  it('si la sesión venció en otra pantalla, al volver a ingresar se llega a Inicio', async () => {
    estado.expirarEnListado = true; abrir('/propiedades')
    fireEvent.change(await screen.findByLabelText(/^Email/), { target: { value: 'admin@sn.local' } })
    estado.expirarEnListado = false
    fireEvent.change(screen.getByLabelText(/Contraseña/), { target: { value: 'Clave-Correcta' } }); fireEvent.click(screen.getByText('Ingresar'))
    expect(await screen.findByRole('heading', { name: /, Paula$/ })).toBeTruthy()
    expect(screen.queryByText('prop-004')).toBeNull()
  })
})

// ---------------- Menú sin subtítulos y "Vivienda" en lugar de "Casa" ----------------
describe('Menú y nombres del panel', () => {
  it('el menú es una sola lista, sin subtítulos de grupo, en este orden', async () => {
    abrir('/'); await screen.findByRole('heading', { name: /, Paula$/ })
    const menu = screen.getByRole('navigation')
    expect(within(menu).getAllByRole('link').map((l) => l.textContent!.trim()))
      .toEqual(['Inicio', 'Propiedades', 'Consultas IA', 'Gestión del sitio web', 'Usuarios', 'Mi cuenta'])
    for (const t of ['Sitio web', 'Clientes', 'Administración', 'Cuenta']) expect(within(menu).queryByText(t)).toBeNull()
    const css = readFileSync('src/ui.module.css', 'utf-8')
    expect(css).not.toMatch(/menuGrupo|menuTitulo/)
  })
  it('el tipo "Casa" se muestra como "Vivienda" (el valor enviado sigue siendo "Casa")', async () => {
    abrir('/propiedades'); await screen.findByText('prop-004')
    const filtro = screen.getByLabelText('Tipo') as HTMLSelectElement
    expect([...filtro.options].map((o) => [o.value, o.textContent])).toEqual([['', 'Todos los tipos'], ['Casa', 'Vivienda'], ['Terreno', 'Terreno']])
    cleanup(); abrir('/propiedades/nueva'); await screen.findByText('Datos de la vivienda')
    const tipo = campo('Tipo') as unknown as HTMLSelectElement
    expect([...tipo.options].map((o) => [o.value, o.textContent])).toEqual([['Casa', 'Vivienda'], ['Terreno', 'Terreno']])
    expect(screen.queryByText(/Datos de la casa/)).toBeNull()
  })
  it('Inicio queda centrado en el alto disponible', () => {
    const css = readFileSync('src/ui.module.css', 'utf-8')
    expect(css).toMatch(/\.inicio \{[^}]*justify-content: center;[^}]*min-height: calc\(100vh - 72px\)/)
  })
})

// ---------------- Gráficos circulares del análisis técnico ----------------
describe('Gráficos circulares', () => {
  it('los porcentajes son enteros y siempre suman 100', () => {
    expect(porcentajes([1, 1, 1])).toEqual([34, 33, 33])
    expect(porcentajes([2, 1])).toEqual([67, 33])
    expect(porcentajes([0, 0])).toEqual([0, 0])
    for (const c of [[7, 3, 5, 1, 9], [1, 2, 3, 4, 5, 6], [13, 13, 13]]) expect(porcentajes(c).reduce((a, b) => a + b, 0)).toBe(100)
  })
  it('muestra las 5 principales y agrupa el resto en "Otras", con cantidad y porcentaje en la leyenda', () => {
    const filas = [9, 8, 7, 6, 5, 3, 2].map((n, i) => ({ clave: `z${i}`, etiqueta: `Zona ${i + 1}`, texto: `Zona ${i + 1}`, cantidad: n }))
    render(<MemoryRouter><Torta titulo="Zonas" filas={filas} /></MemoryRouter>)
    const items = screen.getAllByRole('listitem')
    expect(items.map((li) => li.querySelector('[class*="tortaNombre"]')!.textContent)).toEqual(['Zona 1', 'Zona 2', 'Zona 3', 'Zona 4', 'Zona 5', 'Otras (2)'])
    expect(items[5].textContent).toBe('Otras (2)512 %') // 3 + 2 = 5 de 40
    expect(screen.getByText('40')).toBeTruthy() // total en el centro
    expect(screen.getByRole('img').getAttribute('aria-label')).toContain('Zona 1 9 (23 %)')
    expect(document.querySelectorAll('circle').length).toBe(6)
  })
  it('sin datos muestra un aviso en lugar del gráfico', () => {
    render(<MemoryRouter><Torta titulo="Temas" filas={[{ clave: 'a', etiqueta: 'A', texto: 'A', cantidad: 0 }]} /></MemoryRouter>)
    expect(screen.getByText('Sin datos en este periodo.')).toBeTruthy(); expect(screen.queryByRole('img')).toBeNull()
  })
  it('el medidor de sin servicio muestra la proporción y el resto', () => {
    render(<Medidor valor={1} total={4} etiqueta="Sin servicio" resto="Respondidas" />)
    expect(screen.getByRole('img').getAttribute('aria-label')).toBe('Sin servicio: 1 de 4 (25 %)')
    const items = screen.getAllByRole('listitem')
    expect(items[0].textContent).toBe('Sin servicio125 %'); expect(items[1].textContent).toBe('Respondidas375 %')
  })
})

// ---------------- Mostrar u ocultar contraseñas y recuperación asistida ----------------
describe('Botón del ojo en las contraseñas', () => {
  /** Comprueba el ojo de un campo: alterna password/text, cambia su nombre y no toca el valor. */
  function probarOjo(input: HTMLInputElement) {
    const ojo = input.parentElement!.querySelector('button')!
    expect(ojo.getAttribute('type')).toBe('button')                      // nunca envía el formulario
    fireEvent.change(input, { target: { value: 'Secreta-123' } })
    expect(input.type).toBe('password'); expect(ojo.getAttribute('aria-label')).toBe('Mostrar contraseña')
    fireEvent.click(ojo)
    expect(input.type).toBe('text'); expect(ojo.getAttribute('aria-label')).toBe('Ocultar contraseña'); expect(input.value).toBe('Secreta-123')
    fireEvent.click(ojo)
    expect(input.type).toBe('password'); expect(ojo.getAttribute('aria-label')).toBe('Mostrar contraseña'); expect(input.value).toBe('Secreta-123')
  }
  it('inicio de sesión', async () => {
    estado.sesion = false; abrir('/login'); await screen.findByRole('button', { name: 'Ingresar' })
    probarOjo(campo('Contraseña'))
    expect(ultimo('POST', '/api/admin/auth/login')).toBeUndefined()
  })
  it('cambiar contraseña: los tres campos, cada uno con su propio ojo', async () => {
    abrir('/cuenta/contrasena'); await screen.findByRole('heading', { name: 'Cambiar contraseña' })
    expect(screen.getAllByRole('button', { name: 'Mostrar contraseña' }).length).toBe(3)
    for (const e of ['Contraseña actual', 'Nueva contraseña', 'Repetir la nueva contraseña']) probarOjo(campo(e))
    fireEvent.click(campo('Contraseña actual').parentElement!.querySelector('button')!)
    expect(campo('Contraseña actual').type).toBe('text'); expect(campo('Nueva contraseña').type).toBe('password') // independientes
    expect(ultimo('PUT', '/cuenta/contrasena')).toBeUndefined()
  })
  it('Mi cuenta: la contraseña para confirmar el cambio de email', async () => {
    abrir('/cuenta'); await screen.findByText('Información de la cuenta')
    fireEvent.click(screen.getByLabelText('Editar email'))
    probarOjo(campo(/Contraseña actual/))
  })
  it('los formularios de usuario no piden contraseñas: no hay campos de contraseña en el alta ni en la edición', async () => {
    abrir('/usuarios/nuevo'); await screen.findByRole('heading', { name: 'Nuevo usuario' })
    expect(document.querySelector('input[type="password"]')).toBeNull()
  })
})

describe('Recuperación asistida por el administrador', () => {
  it('avisa que se cerrarán las sesiones y muestra la temporal una vez; nunca muestra la contraseña actual', async () => {
    const confirmar = vi.fn(() => true); vi.stubGlobal('confirm', confirmar)
    abrir('/usuarios/50'); await waitFor(() => expect(campo('Nombre').value).toBe('Vera'))
    expect(document.querySelector('input[type="password"]')).toBeNull()
    fireEvent.click(screen.getByText('Generar contraseña temporal'))
    expect(confirmar).toHaveBeenCalledWith(expect.stringContaining('se cerrarán las sesiones abiertas'))
    expect(await screen.findByText('Nueva8mPq3Rt')).toBeTruthy()
    expect(ultimo('POST', '/api/admin/usuarios/50/contrasena-temporal')!.cuerpo).toBeFalsy() // no envía ninguna contraseña
    Object.assign(usuariosDb.lista[1], { debeCambiarPassword: false })
  })
  it('si cancela, no se genera nada', async () => {
    vi.stubGlobal('confirm', vi.fn(() => false))
    abrir('/usuarios/50'); await waitFor(() => expect(campo('Nombre').value).toBe('Vera'))
    fireEvent.click(screen.getByText('Generar contraseña temporal'))
    expect(ultimo('POST', '/contrasena-temporal')).toBeUndefined()
  })
  it('el gerente y el vendedor no pueden generar contraseñas temporales', async () => {
    for (const rol of ['GERENTE', 'VENDEDOR'] as const) {
      cleanup(); conRol(rol); abrir('/usuarios/50')
      expect(await screen.findByText('No tiene permisos para gestionar usuarios.')).toBeTruthy()
      expect(screen.queryByText('Generar contraseña temporal')).toBeNull()
    }
    expect(ultimo('POST', '/contrasena-temporal')).toBeUndefined()
  })
})

describe('Inicio con imagen de fondo', () => {
  it('la imagen va desenfocada 3 px detrás del contenido, con una capa del color de fondo encima', () => {
    const css = readFileSync('src/ui.module.css', 'utf-8')
    expect(css).toMatch(/\.inicio::before \{[^}]*url\('\.\/assets\/inicio-fondo\.webp'\)[^}]*filter: blur\(3px\)/)
    expect(css).toMatch(/\.inicio::after \{[^}]*color-mix\(in srgb, var\(--fondo\) var\(--velo-inicio\), transparent\)/)
    expect(css).toMatch(/\.inicio::before, \.inicio::after \{[^}]*pointer-events: none/) // no tapa los clics
    const tema = readFileSync('src/global.css', 'utf-8')
    expect(tema.match(/--velo-inicio: 55%/g)!.length).toBe(1); expect(tema.match(/--velo-inicio: 80%/g)!.length).toBe(2) // claro y los dos oscuros
  })
})
