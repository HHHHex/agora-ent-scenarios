package com.agora.entfulldemo.login;

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.agora.entfulldemo.R
import com.agora.entfulldemo.databinding.AppFragmentLoginPhoneInputBinding
import com.agora.entfulldemo.widget.dp
import io.agora.scene.base.component.BaseViewBindingFragment
import io.agora.scene.base.component.OnButtonClickListener
import io.agora.scene.base.component.OnFastClickListener
import io.agora.scene.base.utils.StringUtils
import io.agora.scene.base.utils.ToastUtils
import io.agora.scene.widget.dialog.SwipeCaptchaDialog

class LoginPhoneInputFragment : BaseViewBindingFragment<AppFragmentLoginPhoneInputBinding>() {

    companion object {
        const val Key_Area_Code = "key_area_code"
        const val Key_Account = "key_account"
    }

    private val mLoginViewModel: LoginShareViewModel by activityViewModels()

    private var mSwipeCaptchaDialog: SwipeCaptchaDialog? = null

    override fun getViewBinding(inflater: LayoutInflater, container: ViewGroup?): AppFragmentLoginPhoneInputBinding {
        return AppFragmentLoginPhoneInputBinding.inflate(inflater)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d("zhangw", "LoginPhoneInputFragment onAttach")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("zhangw", "LoginPhoneInputFragment onDestroy")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d("zhangw", "LoginPhoneInputFragment onCreateView")
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("zhangw", "LoginPhoneInputFragment onViewCreated")
        setOnApplyWindowInsetsListener(binding.root)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context?.mainLooper?.queue?.addIdleHandler {
                Log.d("addIdleHandler", "showKeyboard -- queueIdle -- 1")
                binding.etAccounts.isFocusable = true
                binding.etAccounts.isFocusableInTouchMode = true
                binding.etAccounts.requestFocus()
                showKeyboard(binding.etAccounts)
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("zhangw", "LoginPhoneInputFragment onDestroyView")
        mLoginViewModel.clearDispose()
    }

    override fun initListener() {
        super.initListener()
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        })
        binding.etAccounts.doAfterTextChanged {
            binding.iBtnClearAccount.isGone = it.isNullOrEmpty()
        }
        binding.etAccounts.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.llInput.setBackgroundResource(R.drawable.app_login_input_border_on)
                val padding = 2.dp.toInt()
                binding.llInput.setPadding(padding, padding, padding, padding)
                binding.btnVerify.alpha = 1.0f
                binding.btnVerify.isEnabled = true
            } else {
                binding.llInput.setBackgroundResource(R.drawable.app_login_input_border_off)
                val padding = 1.dp.toInt()
                binding.llInput.setPadding(padding, padding, padding, padding)
                binding.btnVerify.alpha = 0.6f
                binding.btnVerify.isEnabled = false
            }
        }
        binding.btnBack.setOnClickListener(object : OnFastClickListener() {
            override fun onClickJacking(view: View) {
                findNavController().popBackStack()
            }
        })
        binding.btnVerify.setOnClickListener(object : OnFastClickListener() {

            override fun onClickJacking(view: View) {
                val account = binding.etAccounts.text.toString()
                if (!StringUtils.checkPhoneNum(account)) {
                    ToastUtils.showToast(R.string.app_input_phonenum_tip)
                } else {
                    showSwipeCaptchaDialog(account)
                }
            }
        })
        binding.iBtnClearAccount.setOnClickListener(object : OnFastClickListener() {

            override fun onClickJacking(view: View) {
                binding.etAccounts.setText("")
            }
        })
    }

    override fun requestData() {
        super.requestData()
        mLoginViewModel.mRequestCodeLiveData.observe(this) {
            if (it) {
                Log.d("zhangw", "LoginPhoneInputFragment mRequestCodeLiveData true")
                if (findNavController().currentDestination?.id == R.id.fragmentPhoneInput) {
                    findNavController().navigate(R.id.action_fragmentPhoneInput_to_fragmentVerify, Bundle().apply {
                        putString(Key_Account, mLoginViewModel.getPhone())
                    })
                }
            }
        }
    }

    private fun showSwipeCaptchaDialog(account: String) {
        val cxt = context ?: return
        if (mSwipeCaptchaDialog == null) {
            mSwipeCaptchaDialog = SwipeCaptchaDialog(cxt)
        }
        mSwipeCaptchaDialog?.onButtonClickListener = object : OnButtonClickListener {
            override fun onLeftButtonClick() {}
            override fun onRightButtonClick() {
                mLoginViewModel.requestSendVCode(account)
            }
        }
        mSwipeCaptchaDialog?.show()
    }
}
