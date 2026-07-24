package com.tuapp.ventas.ui.estadisticas

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/** Adaptador del ViewPager2 con las dos pestañas de estadísticas. */
class EstadisticasPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> ResumenDiaFragment()
        1 -> ExistenciaDiaFragment()
        else -> ResumenDiaFragment()
    }
}
