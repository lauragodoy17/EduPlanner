package com.softcg.myapplication.ui.Inicio.Fragments.Ayuda

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.softcg.myapplication.R


class HelpFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view= inflater.inflate(R.layout.fragment_help, container, false)
        initQueja(view)
        initQuejados(view)
        return view
    }
    private fun initQueja(view: View){

        val quejas=view.findViewById<AutoCompleteTextView>(R.id.QuejaEditText)
        val quejados=view.findViewById<AutoCompleteTextView>(R.id.QuejadosEditText)
        val descripcion=view.findViewById<EditText>(R.id.DescripcionEditText)
        val guardarBoton= view.findViewById<Button>(R.id.botonAgregar)

        val items= listOf("Problemas técnicos", "Problemas de usabilidad", "Problemas de contenido", "Problemas de comunicación", "Problemas de seguridad", "Problemas administrativos")
        val autoComplete =view.findViewById<AutoCompleteTextView>(R.id.QuejaEditText)
        val adapter= ArrayAdapter(requireContext(), R.layout.item_menu_asignatura,items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener= AdapterView.OnItemClickListener{
                adapterView, view, i, l ->
        }
        guardarBoton.setOnClickListener {
            Toast.makeText(requireContext()," Comentario guardado", Toast.LENGTH_SHORT).show()
        }
    }
    private fun initQuejados(view: View){

        val quejas=view.findViewById<AutoCompleteTextView>(R.id.QuejaEditText)
        val quejados=view.findViewById<AutoCompleteTextView>(R.id.QuejadosEditText)
        val descripcion=view.findViewById<EditText>(R.id.DescripcionEditText)
        val guardarBoton= view.findViewById<Button>(R.id.botonAgregar)

        val items= listOf("La aplicación no se carga", "Funcionalidades que no funcionan", "Problemas con la interfaz de usuario", "La aplicación es lenta o se bloquea", "Alto consumo de batería", "Interfaz confusa o difícil de entender", "Problemas para encontrar ciertas funcionalidades","Dificultad para personalizar horarios o eventos", "Problemas al agregar, editar o eliminar eventos/tareas", "Notificaciones de eventos/tareas que no funcionan correctamente", "Problemas al visualizar el calendario semanal", "Confusión con zonas horarias y fechas", "No recibir notificaciones importantes", "Problemas al iniciar sesión o recuperar contraseñas", "Dificultad para crear o eliminar cuentas de usuario")
        val autoComplete =view.findViewById<AutoCompleteTextView>(R.id.QuejadosEditText)
        val adapter= ArrayAdapter(requireContext(), R.layout.item_menu_asignatura,items)
        autoComplete.setAdapter(adapter)
        autoComplete.onItemClickListener= AdapterView.OnItemClickListener{
                adapterView, view, i, l ->
        }
        guardarBoton.setOnClickListener {
            Toast.makeText(requireContext()," Comentario guardado", Toast.LENGTH_SHORT).show()
        }
    }
}