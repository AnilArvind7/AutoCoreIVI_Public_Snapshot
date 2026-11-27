package com.example.autocoreividashboard.vhal

object VhalRuntime {
    val mockVhal: MockVhalImpl by lazy {
        MockVhalImpl()
    }

    val controller: MockVhalController by lazy {
        MockVhalController(mockVhal)
    }

    val propertyAdapter: VhalPropertyAdapter by lazy {
        VhalPropertyAdapter(mockVhal)
    }
}
