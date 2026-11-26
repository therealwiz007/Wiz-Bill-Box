package com.wizlabs.billbox.ui.mascot

import com.wizlabs.billbox.AppViewModel
import com.wizlabs.billbox.SyncState
import com.wizlabs.billbox.engine.TallyContext
import com.wizlabs.billbox.model.AppData
import com.wizlabs.billbox.ui.screens.BillQuery

/** Builds the brain's input from the app's real state. */
fun tallyContext(data: AppData, sync: SyncState, searchNoResults: Boolean = false, justSaved: Boolean = false, justUploaded: Boolean = false): TallyContext {
    val signedIn = data.account != null
    return TallyContext(
        bills = data.bills.size,
        signedIn = signedIn,
        uploading = if (signedIn && sync is SyncState.Running) data.bills.count { it.dirty }.coerceAtLeast(1) else 0,
        offline = sync is SyncState.Failed && sync.offline,
        syncError = sync is SyncState.Failed && !sync.offline,
        expiringSoon = BillQuery.warrantySoon(data.bills, 30).size,
        recentlyExpired = BillQuery.recentlyExpired(data.bills)?.product,
        searchNoResults = searchNoResults, justSaved = justSaved, justUploaded = justUploaded,
    )
}
