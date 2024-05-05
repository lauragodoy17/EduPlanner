package com.softcg.myapplication.ui.home.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.softcg.myapplication.R
import com.softcg.myapplication.data.Repositories.TareasRepository
import com.softcg.myapplication.data.database.TareasDatabase.TareasDatabase
import com.softcg.myapplication.data.database.dao.TareasDao
import com.softcg.myapplication.databinding.FragmentHomeBinding
import com.softcg.myapplication.ui.home.ViewModelHome
import com.softcg.myapplication.ui.home.adapters.TareasAdapter
import com.softcg.myapplication.ui.tarea.model.Tarea
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModelHome: ViewModelHome by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.fragment_home, container, false)

        //Recycler
        viewModelHome.obtenerTareas()

        val adapter=TareasAdapter()
        val recyclerView=view.findViewById<RecyclerView>(R.id.recyclerclases)
        recyclerView.adapter=adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        //
        viewModelHome.tareas.observe(viewLifecycleOwner, Observer {Tarea ->
            adapter.setData(Tarea)
            if (Tarea.isNotEmpty()){
                view.findViewById<TextView>(R.id.textonoclases).visibility=View.GONE
            }
        })

        return view
    }

}