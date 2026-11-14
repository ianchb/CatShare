package moe.reimu.catshare.exceptions

import kotlinx.coroutines.CancellationException

class CancelledByUserException(val isRemote: Boolean) :
    CancellationException("Cancelled by user")